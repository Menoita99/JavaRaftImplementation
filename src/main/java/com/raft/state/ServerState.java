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

import com.raft.models.Address;
import com.raft.models.Log;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * This class represents volatile and non-volatile server state
 * @author RuiMenoita
 *
 */
@Data
public class ServerState implements Serializable{
	private static final long serialVersionUID = 1L;

	private static final String STATE_FILE = "src/main/resources/state.conf";
	private static final String LOG_FILE = "src/main/resources/log.txt";


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
	private Log lastLog;

	// Volatile state
	private long commitIndex = 0;
	private long lastApplied = 0;


	
	public ServerState() throws IOException {
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
		currentTerm = Long.parseLong((String) stateProperties.getOrDefault("currentTerm", "0"));
		votedFor = Address.parse((String) stateProperties.getOrDefault("votedFor", null));
		
		File logFile = new File(LOG_FILE);
		logFile.createNewFile();
		logWriter = new PrintWriter(logFile);
	}



	
	/**
	 * Set and store's in the hard drive the current term
	 */
	public void setCurrentTerm(long currentTerm) {
		try {
			this.currentTerm = currentTerm;
			stateProperties.setProperty("currentTerm", currentTerm+"");
			stateProperties.store(new FileOutputStream(STATE_FILE), null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




	/**
	 * Set and store's in the hard drive the last voted for server
	 */
	public void setVotedFor(Address votedFor) {
		try {
			this.votedFor = votedFor;
			stateProperties.setProperty("votedFor", votedFor+"");
			stateProperties.store(new FileOutputStream(STATE_FILE), null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}





	/**
	 * Appends a new entry in the logs file and set's last log 
	 */
	public void appendLog(Log log) {
		lastLog = log;
		if(currentTerm != log.getTerm())
			setCurrentTerm(log.getTerm());
		logWriter.println(log);
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
				//TODO
				s.skip("");
				String line = s.nextLine();
				return line.isBlank() ? false : true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
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
		}
	}
}
