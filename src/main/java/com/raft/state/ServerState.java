package com.raft.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.raft.interpreter.Interpreter;
import com.raft.models.Address;
import com.raft.models.Entry;
import com.raft.models.Snapshot;

import lombok.Data;
import lombok.ToString.Exclude;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.commons.io.input.ReversedLinesFileReader;

/**
 * This class represents volatile and non-volatile server state
 * @author RuiMenoita
 *
 */
@Data
public class ServerState implements Serializable{
	private static final long serialVersionUID = 1L;

	public final static String STATE_FILE = "state.conf"; 
	public final static String LOG_FILE = "log.txt";

	private String stateFilePath = "src/main/resources/"+STATE_FILE;
	private String logFilePath = "src/main/resources/"+LOG_FILE;

	private transient Properties stateProperties;
	private transient PrintWriter logWriter;

	//Stable state
	private long currentTerm = 0;
	private Address votedFor;
	private long commitIndex = 0;
	private Entry lastAplied = new Entry(0,0,null,null);

	// Volatile state
	private Vector<Entry> log = new Vector<>();
	private Entry lastEntry = new Entry(0,0,null,null);

	private ReentrantLock lock = new ReentrantLock();
	private ReentrantLock persistenceStateLock = new ReentrantLock();
	private ReentrantLock logLock = new ReentrantLock();

	private Interpreter interpreter = new Interpreter();

	private long entryCounter = 0;

	private String rootPath;

	@Exclude
	@lombok.EqualsAndHashCode.Exclude
	private Snapshot snapshot;


	public ServerState(String rootPath) throws IOException {
		setRootPath(rootPath);
		init(); 
		snapshot = new Snapshot(this);
	}




	/**
	 * Reads configuration files and start object attributes
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void init() {
		try {
			new File(stateFilePath).createNewFile();
			new File(logFilePath).createNewFile();

			stateProperties = new Properties();
			stateProperties.load(new FileInputStream(stateFilePath));

			long fileTerm = Long.parseLong((String) stateProperties.getOrDefault("currentTerm", "0"));
			if(currentTerm == 0 || fileTerm!=0) {
				setCurrentTerm(fileTerm);
				setVotedFor(Address.parse((String) stateProperties.getOrDefault("votedFor", "")));
			}

			log.clear();
			logWriter = new PrintWriter(new FileOutputStream(new File(logFilePath),true));

			recoverState();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void recoverState() throws TimeoutException, InterruptedException, IOException {
		//check log file and recover the entry list
		List<Entry> entries = new LinkedList<>();
		Files.lines(Paths.get(logFilePath)).forEach(line -> {
			Entry e = Entry.fromString(line);
			if(e!=null)
				entries.add(e);
		});
		interpreter.getPool().resuscitateDeadWorkers();
		//submit the entry list
		if(!entries.isEmpty()) {
			interpreter.submit(entries);
			//wait for the result of the last entry
			Entry last = entries.get(entries.size()-1);
			interpreter.getCommandResult(last.getCommandID(), 0);
			lastEntry = last;
			lastAplied = last;
			commitIndex = last.getIndex();
		}
		System.out.println("State Recovered -> processed "+entries.size()+" entries to recover");
	}




	/**
	 * Set and store's in the hard drive the current term
	 */
	public void setCurrentTerm(long currentTerm) {
		try {
			persistenceStateLock.lock();
			this.currentTerm = currentTerm;
			stateProperties.setProperty("currentTerm", currentTerm+"");
			stateProperties.store(new FileOutputStream(stateFilePath), null);
			setVotedFor(null);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			persistenceStateLock.unlock();
		}
	}




	/**
	 * Set and store's in the hard drive the last voted for server
	 */
	public void setVotedFor(Address votedFor) {
		try {
			persistenceStateLock.lock();
			this.votedFor = votedFor;
			stateProperties.setProperty("votedFor", votedFor == null ? "":votedFor.toFileString());
			stateProperties.store(new FileOutputStream(stateFilePath), null);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			persistenceStateLock.unlock();
		}
	}





	public Entry createEntry(String command, String commandID) {
		try {
			lock.lock();
			Entry entry = new Entry(lastEntry.getIndex()+1, currentTerm, command, commandID);
			lastEntry = entry;
			log.add(entry);
			return entry;
		}finally {
			lock.unlock();
		}
	}






	public void setCommitIndex(long index) {
		try {
			//			System.out.println("Called commit Index");
			lock.lock();

			if(commitIndex>index)
				return;

			if(lastEntry.getIndex()<index) 
				throw new IllegalStateException("Last log index "+lastEntry.getIndex()+" can't be lower then commited log "+index);

			List<Entry> commitedEntries = new ArrayList<>();
			log.sort((o1,o2) -> ((Long)(o1.getIndex()-o2.getIndex())).intValue());

			for (Entry entry : log) {
				if(entry.getIndex()<=index) {
					commitedEntries.add(entry);
					if(lastAplied.getIndex()<entry.getIndex())
						lastAplied=entry;
				}
			}

			saveEntries(commitedEntries);
			commitIndex = index;
			interpreter.submit(commitedEntries);
			log.removeAll(commitedEntries);

			assessSnapshot(commitedEntries.size());
		}finally {
			lock.unlock();
			//			System.out.println("Exiting commit Index");
		}
	}




	public void assessSnapshot(long size) {
		entryCounter = entryCounter + size;
		if (entryCounter >= 20_000) {
			entryCounter = 0;
			snapshot.snap();
		}
	}





	private void saveEntries(List<Entry> commitedEntries) {
		try {
			logLock.lock();
			String content = commitedEntries.stream().map(e-> e.toFileString()).collect(Collectors.joining("\n"));
			logWriter.println(content);
			logWriter.flush();
		}finally {
			logLock.unlock();
		}
	}




	public void addEntry(Entry entry) {
		if(!log.contains(entry)) {
			lastEntry = entry;
			log.add(entry);
		}
	}





	/**
	 * Verifies if a certain log with term and index given was already been stored
	 * @param term certain log term
	 * @param index certain log term
	 * @return true if there is this log in memory or false otherwise
	 */
	public boolean hasLog(long term, long index) {
		return	index > lastEntry.getIndex() ? false : getEntry(index).getTerm() == term;
	}








	/**
	 * Overrides the given log in memory
	 * @param entry log to be written
	 */
	public void override(Entry entry) {
		lock.lock();
		try {
			for (int i = 0 ; i < log.size(); i++) {
				if(log.get(i).getIndex() == entry.getIndex()) {
					log.set(i, entry);
					return;
				}
			}
		}finally {
			lock.unlock();
		}

		logLock.lock();
		try{
			List<String> fileContent = new ArrayList<>(Files.readAllLines(Paths.get(logFilePath)));
			for (int i = 0; i < fileContent.size(); i++) {
				if (Entry.fromString(fileContent.get(i)).getIndex() == entry.getIndex()) {
					fileContent.set(i, entry.toFileString());
					fileContent = fileContent.subList(0, i+1);
					break;
				}
			}

			logWriter.close();
			Files.write(Paths.get(logFilePath), fileContent);
			logWriter =  new PrintWriter(new FileOutputStream(new File(logFilePath),true));
			log.clear();
			lastEntry = entry;
			lastAplied = entry;
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			logLock.unlock();
		}

	}



	public LinkedList<Entry> getEntriesSince(long nextIndex, int chunckSize) {
		if(chunckSize<=0) throw new IllegalStateException("Chunk must be bigger then 0");
		if(nextIndex == 0) nextIndex = 1;

		LinkedList<Entry> entries = new LinkedList<>();

		logLock.lock();
		try(ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(logFilePath),Charset.defaultCharset())){
			String line = "";
			long smalestIndex = Long.MAX_VALUE;
			while((line = reader.readLine()) != null) {
				Entry e = Entry.fromString(line);
				if(e != null) {
					long index = e.getIndex();
					if(index < chunckSize+nextIndex && index >= nextIndex && entries.size()<chunckSize) 
						entries.add(e);
					if(index < smalestIndex)
						smalestIndex = index;
				}
			}
			if(smalestIndex>nextIndex) 
				return null;
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			logLock.unlock();
		}

		if(entries.size()<chunckSize && !log.isEmpty()) {
			log.sort((o1,o2) -> ((Long)(o1.getIndex()-o2.getIndex())).intValue());
			Entry first = log.get(0);
			if(entries.isEmpty() && first.getIndex() == nextIndex)
				entries.addAll(log);
			else if(!entries.isEmpty() && first.getIndex() == entries.get(entries.size()-1).getIndex()+1)
				entries.addAll(log);
			if(entries.size()>chunckSize)
				entries.subList(0, chunckSize);
		}
		return entries;
	}





	public Entry getEntry(long index) {
		if(index>lastEntry.getIndex())
			return null;

		if(lastEntry.getIndex() == index ) return lastEntry;
		if(lastAplied.getIndex() == index) return lastAplied;

		if(index <= 0)
			return new Entry(0,0,null,null);

		try {
			lock.lock();
			for (Entry entry : log) 
				if(entry.getIndex() == index)
					return entry;
		}finally {
			lock.unlock();
		}

		logLock.lock();
		try(ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(logFilePath),Charset.defaultCharset())){
			String line = "";
			while((line = reader.readLine()) != null) {
				Entry e =  Entry.fromString(line);
				if(e.getIndex() == index)
					return e;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			logLock.unlock();
		}

		return null;
	}




	public void clearLogFile() throws IOException {
		logLock.lock();
		try {
			if(logWriter!= null)logWriter.close();
			System.out.println("CLEARING LOG ");
			File file = new File(logFilePath);
			if(file.delete())
				file.createNewFile();
			logWriter = new PrintWriter(new FileOutputStream(new File(logFilePath),true));
		} finally {
			logLock.unlock();
		}
	}

	
	
	
	public void setRootPath(String path) {
		rootPath = path;
		stateFilePath = rootPath+"/"+STATE_FILE;
		logFilePath = rootPath+"/"+LOG_FILE;
	}
	
	
	
	
	public void close(){
		logWriter.close();
	}
}
