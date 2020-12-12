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
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.raft.interpreter.Interpreter;
import com.raft.models.Address;
import com.raft.models.Entry;
import com.raft.models.Snapshot;

import lombok.Data;
import lombok.ToString.Exclude;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
	private Entry lastAplied;

	// Volatile state
	private Vector<Entry> log = new Vector<>();
	private Entry lastEntry;

	private ReentrantLock lock = new ReentrantLock();
	private ReentrantReadWriteLock persistenceStateLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();

	private Interpreter interpreter = new Interpreter();

	private long entryCounter;

	private String rootPath;

	@Exclude
	@lombok.EqualsAndHashCode.Exclude
	private Snapshot snapshot;


	public ServerState(String rootPath) throws IOException {
		this.rootPath = rootPath;
		stateFilePath = rootPath+File.separator+STATE_FILE;
		logFilePath = rootPath+File.separator+LOG_FILE;
		init(); 
		snapshot = new Snapshot(this);
	}




	/**
	 * Reads configuration files and start object attributes
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public void init() throws IOException, FileNotFoundException {
		new File(stateFilePath).createNewFile();
		new File(logFilePath).createNewFile();

		stateProperties = new Properties();
		stateProperties.load(new FileInputStream(stateFilePath));

		setCurrentTerm(Long.parseLong((String) stateProperties.getOrDefault("currentTerm", "0")));
		setVotedFor(Address.parse((String) stateProperties.getOrDefault("votedFor", "")));

		try(ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(logFilePath),Charset.defaultCharset())){
			Entry last = Entry.fromString(reader.readLine());
			if(last == null)
				last = new Entry(0,0,null,null);
			lastEntry = last;
			lastAplied = last;
			commitIndex = lastAplied.getIndex();
			reader.close();
		}
		logWriter = new PrintWriter(new FileOutputStream(new File(logFilePath),true));

		entryCounter=0;
	}




	/**
	 * Set and store's in the hard drive the current term
	 */
	public void setCurrentTerm(long currentTerm) {
		try {
			persistenceStateLock.writeLock().lock();
			this.currentTerm = currentTerm;
			stateProperties.setProperty("currentTerm", currentTerm+"");
			stateProperties.store(new FileOutputStream(stateFilePath), null);
			setVotedFor(null);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			persistenceStateLock.writeLock().unlock();
		}
	}




	/**
	 * Set and store's in the hard drive the last voted for server
	 */
	public void setVotedFor(Address votedFor) {
		try {
			persistenceStateLock.writeLock().lock();
			this.votedFor = votedFor;
			stateProperties.setProperty("votedFor", votedFor == null ? "":votedFor.toFileString());
			stateProperties.store(new FileOutputStream(stateFilePath), null);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			persistenceStateLock.writeLock().unlock();
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
			lock.lock();

			if(commitIndex>index)
				return;

			if(lastEntry.getIndex()<index) 
				throw new IllegalStateException("Last log index "+lastEntry.getIndex()+" can't be lower then commited log "+index);

			List<Entry> commitedEntries = new ArrayList<>();
			//			Vector<Entry> clone = (Vector<Entry>) log.clone();
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
		}
	}


	//TO TEST
	public static void main(String[] args) throws IOException {
		ServerState st = new ServerState( "src/main/resources/Server1000");
		st.assessSnapshot(2000);
	}
	

	public void assessSnapshot(long size) {
		entryCounter = entryCounter + size;
		if (entryCounter >= 1000) {
			entryCounter = 0;
			new Thread(snapshot::snap).start();
		}
	}





	private void saveEntries(List<Entry> commitedEntries) {
		try {
			logLock.writeLock().lock();
			commitedEntries.forEach(entry -> logWriter.println(entry.toFileString()));
			logWriter.flush();
		}finally {
			logLock.writeLock().unlock();
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
		return index > lastEntry.getIndex() ? false : getEntry(index).getTerm() == term;
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

		logLock.writeLock().lock();
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
			logWriter = new PrintWriter(new FileOutputStream(new File(logFilePath),true));
			log.clear();
			lastEntry = entry;
			lastAplied = entry;
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			logLock.writeLock().unlock();
		}

	}






	public List<Entry> getEntriesSince(long nextIndex, int chunckSize) {
		if(nextIndex == 0) nextIndex = 1;

		List<Entry> entries = new ArrayList<>();

		logLock.readLock().lock();
		try(ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(logFilePath),Charset.defaultCharset())){
			String line = "";
			while((line = reader.readLine()) != null) {
				Entry e =  Entry.fromString(line);
				if((e.getIndex()>=nextIndex && chunckSize < 0) ||  (e.getIndex()>=nextIndex && e.getIndex()<nextIndex+chunckSize && chunckSize > 0 &&  entries.size() < chunckSize ))
					entries.add(e);

				if(chunckSize > 0 && entries.size() > chunckSize || (e.getIndex()<nextIndex && chunckSize < 0) )
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			logLock.readLock().unlock();
		}


		if(entries.size() < chunckSize || chunckSize < 0) {
			if(entries.isEmpty())
				entries.addAll(log);
			else {
				log.sort((o1,o2) -> ((Long)(o1.getIndex()-o2.getIndex())).intValue());
				if(log.get(0).getIndex() == entries.get(entries.size()-1).getIndex()+1)
					entries.addAll(log);
			}
			if(entries.size() > chunckSize && chunckSize >=0)
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


		try(ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(logFilePath),Charset.defaultCharset())){
			logLock.readLock().lock();
			String line = "";
			while((line = reader.readLine()) != null) {
				Entry e =  Entry.fromString(line);
				if(e.getIndex() == index)
					return e;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			logLock.readLock().unlock();
		}

		return null;
	}
}
