package com.raft.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;

import com.raft.interpreter.Interpreter;
import com.raft.models.Address;
import com.raft.models.Entry;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class represents volatile and non-volatile server state
 * @author RuiMenoita
 *
 */
@Data
public class ServerState implements Serializable{
	private static final long serialVersionUID = 1L;

	//	private static final String STATE_FILE = "src/main/resources/state.conf";
	//	private static final String LOG_FILE = "src/main/resources/log.txt";
	private  String STATE_FILE = "src/main/resources/state";
	private  String LOG_FILE = "src/main/resources/log";


	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private Properties stateProperties;
	@Setter(value = AccessLevel.NONE)
	@Getter(value = AccessLevel.NONE)
	private PrintWriter logWriter;

	//Stable state
	private long currentTerm = 0;
	private Address votedFor;
	private Vector<Entry> log = new Vector<>();

	// Volatile state
	private long commitIndex = 0;
	private Entry lastEntry = new Entry(0,0,null,null);

	private ReentrantLock lock = new ReentrantLock();
	private ReentrantReadWriteLock persistenceStateLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();

	private Interpreter interpreter = new Interpreter();




	public ServerState() throws IOException {
		//just to test ( to have 3 different log.txt and state.conf files)
		int r = (int)(Math.random()*90000);
		STATE_FILE = STATE_FILE+r+".conf";
		LOG_FILE = LOG_FILE+r+".txt";
		init(); 
	}




	/**
	 * Reads configuration files and start object attributes
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void init() throws IOException, FileNotFoundException {
		stateProperties = new Properties();
		new File(STATE_FILE).createNewFile();
		stateProperties.load(new FileInputStream(STATE_FILE));
		setCurrentTerm(Long.parseLong((String) stateProperties.getOrDefault("currentTerm", "0")));
		setVotedFor(Address.parse((String) stateProperties.getOrDefault("votedFor", new Address("", -1).toFileString())));

		File logFile = new File(LOG_FILE);
		logFile.createNewFile();
		logWriter = new PrintWriter(logFile);
	}




	/**
	 * Set and store's in the hard drive the current term
	 */
	public void setCurrentTerm(long currentTerm) {
		try {
			persistenceStateLock.writeLock().lock();
			this.currentTerm = currentTerm;
			stateProperties.setProperty("currentTerm", currentTerm+"");
			stateProperties.store(new FileOutputStream(STATE_FILE), null);
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
			stateProperties.setProperty("votedFor", votedFor.toFileString());
			stateProperties.store(new FileOutputStream(STATE_FILE), null);
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			persistenceStateLock.writeLock().unlock();
		}
	}





	public Entry createEntry(String command, String commandID) {
		try {
			lock.lock();
			Entry entry = new Entry( lastEntry.getIndex()+1, currentTerm, command, commandID);
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
			for (Entry entry : log) {
				if(entry.getIndex()<=index) {
					commitedEntries.add(entry);
					saveEntry(entry);
				}
			}

			commitIndex = index;
			interpreter.submit(commitedEntries);
			log.removeAll(commitedEntries);
		}finally {
			lock.unlock();
		}
	}








	public void addEntry(Entry entry) {
		try {
			lock.lock();
			if(!log.contains(entry)) {
				lastEntry = entry;
				log.add(entry);
			}
		}finally {
			lock.unlock();
		}
	}








	/**
	 * Appends a new entry in the logs file and set's last log 
	 */
	private void saveEntry(Entry entry) {
		try {
			logLock.writeLock().lock();
			if(currentTerm != entry.getTerm())
				setCurrentTerm(entry.getTerm());
			logWriter.println(entry.toFileString());
			logWriter.flush();
		}finally {
			logLock.writeLock().unlock();
		}
	}





	/**
	 * Verifies if a certain log with term and index given was already been stored
	 * @param term certain log term
	 * @param index certain log term
	 * @return true if there is this log in memorr or false otherwise
	 */
	public boolean hasLog(long term, long index) {
		if(term==lastEntry.getTerm() && index==lastEntry.getIndex()) 
			return true;
		if(term>lastEntry.getTerm() && index>lastEntry.getIndex()) 
			return false;
		if(term<lastEntry.getTerm() || index<lastEntry.getIndex()) {
			try(Scanner s = new Scanner(new File(LOG_FILE))){
				logLock.readLock().lock();
				//TODO
				s.skip("");
				String line = s.nextLine();
				return line.isBlank() ? false : true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}finally {
				logLock.readLock().unlock();
			}
			return false;
		}
		return false;
	}








	/**
	 * Overrides the given log in memory
	 * @param entry log to be written
	 */
	public void override(Entry entry) {
		log.removeIf((oldEntry) -> oldEntry.getIndex() == entry.getIndex());
		log.add(entry);
	}




	public List<Entry> getEntriesSince(long nextIndex) {
		List<Entry> entries = new ArrayList<>();

		lock.lock();

		//if log has all requested entries in memory
		log.sort((o1,o2) -> ((Long)(o1.getIndex()-o2.getIndex())).intValue());
		if(!log.isEmpty() && log.get(0).getIndex() <= nextIndex) {
			int logIndex = (int) (nextIndex - log.get(0).getIndex());
			entries.addAll(log.subList(logIndex-1, log.size()));
			return entries;
		}

		lock.unlock();

		try(Scanner s = new Scanner(new File(LOG_FILE))){
			logLock.readLock().lock();
			while(s.hasNext()) {
				Entry e =  Entry.fromString(s.nextLine());
				if(e.getIndex()>=nextIndex)
					entries.add(e);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}finally {
			logLock.readLock().unlock();
		}

		entries.addAll(log);
		return entries;
	}




	public Entry getEntry(long index) {
		if(index <= 0)
			return new Entry(0,0,null,null);

		lock.lock();

		for (Entry entry : log) 
			if(entry.getIndex() == index)
				return entry;

		lock.unlock();

		try(Scanner s = new Scanner(new File(LOG_FILE))){
			logLock.readLock().lock();
			while(s.hasNext()) {
				Entry e =  Entry.fromString(s.nextLine());
				if(e.getIndex() == index)
					return e;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}finally {
			logLock.readLock().unlock();
		}

		return null;
	}

}
