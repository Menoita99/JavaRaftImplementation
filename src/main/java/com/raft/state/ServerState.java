package com.raft.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Properties;
import java.util.Scanner;

import com.raft.interpreter.Interpreter;
import com.raft.models.Address;
import com.raft.models.Log;

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
	@Setter(value = AccessLevel.NONE)
	private Log lastLog = new Log(0,0,null,null);

	// Volatile state
	private long commitIndex = 0;
	private long lastApplied = 0;

	private Interpreter interpreter = new Interpreter();

	private ReentrantLock lock = new ReentrantLock();
	private ReentrantReadWriteLock persistenceStateLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock logLock = new ReentrantReadWriteLock();

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





	/**
	 * Appends a new entry in the logs file and set's last log 
	 */
	public void appendLog(Log log) {
		try {
			logLock.writeLock().lock();
			lastLog = log;
			if(currentTerm != log.getTerm())
				setCurrentTerm(log.getTerm());
			logWriter.println(log.toFileString());
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
		if(term==lastLog.getTerm() && index==lastLog.getIndex()) 
			return true;
		if(term>lastLog.getTerm() && index>lastLog.getIndex()) 
			return false;
		if(term<lastLog.getTerm() || index<lastLog.getIndex()) {
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
	 * @param log log to be written
	 */
	public void override(Log log) {
		try {
			logLock.writeLock().lock();
			File temp = File.createTempFile("temp", ".tmp");
			File logFile = new File(LOG_FILE);
			try(PrintWriter pw = new PrintWriter(temp)) {
				try(Scanner s = new Scanner(logFile)){
					while (s.hasNext()) {
						String line = s.nextLine();
						//TODO
						pw.println(line);
					}
					temp.renameTo(logFile);
				}
			}
		}catch (IOException e) {
			e.printStackTrace();
		}finally {
			logLock.writeLock().unlock();
		}
	}
}
