package com.raft.state;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Properties;

import com.raft.models.Address;
import com.raft.models.Log;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
		stateProperties = new Properties();
		stateProperties.load(new FileInputStream(STATE_FILE));
		File logFile = new File(LOG_FILE);
		logFile.createNewFile();
		logWriter = new PrintWriter(logFile); 
	}





	public void setCurrentTerm(long currentTerm) {
		try {
			this.currentTerm = currentTerm;
			stateProperties.setProperty("currentTerm", currentTerm+"");
			stateProperties.store(new FileOutputStream(STATE_FILE), null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}





	public void setVotedFor(Address votedFor) {
		try {
			this.votedFor = votedFor;
			stateProperties.setProperty("votedFor", votedFor+"");
			stateProperties.store(new FileOutputStream(STATE_FILE), null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}






	public void appendLog(Log log) {
		lastLog = log;
		if(currentTerm != log.getTerm())
			setCurrentTerm(log.getTerm());
		logWriter.println(log);
	}





	public boolean hasLog(long term, long index) {
		if(term==lastLog.getTerm() && index==lastLog.getIndex()) 
			return true;
		if(term>lastLog.getTerm() && index>lastLog.getIndex()) 
			return false;
		if(term<lastLog.getTerm() || index<lastLog.getIndex()) {
			//TODO
			return false;
		}
		return false;
	}





	public void override(Log log) {
		// TODO Auto-generated method stub

	}
}
