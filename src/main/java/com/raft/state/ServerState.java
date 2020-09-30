package com.raft.state;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;

import com.raft.models.Address;
import com.raft.models.Log;

import lombok.Data;

@Data
public class ServerState implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private static final String STATE_FILE = "src/main/resources/state.conf";
	private PrintWriter writer;
	
	//Stable state
	private long currentTerm = 0;
	private Address votedFor;
	private Log lastLog;
	
	// Volatile state
	private long commitIndex = 0;
	private long lastApplied = 0;
	
	
	public ServerState() throws IOException {
		File file = new File("");
		file.createNewFile();
		writer = new PrintWriter(STATE_FILE);
	}
	
	
	
	
	
	public void setCurrentTerm(long currentTerm) {
		//TODO save using writes;
		this.currentTerm = currentTerm;
	}
	
	
	
	
	
	public void setVotedFor(Address votedFor) {
		//TODO save using writes;
		this.votedFor = votedFor;
	}
	
	
	
	
	public void setLastLog(Log lastLog) {
		//TODO append long using writer
		this.lastLog = lastLog;
	}
}
