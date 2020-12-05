package com.monitor;

import java.io.Serializable;

import com.raft.Server;
import com.raft.models.Address;
import com.raft.models.Entry;
import com.raft.state.Mode;
import com.raft.state.ServerState;

import lombok.Data;

@Data
public class MonitorRequest implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private Address sender;
	private Mode mode;
	private long currentTerm;
	private Address votedFor;
	private long commitIndex;
	private Entry lastEntry;
	private Entry lastAplied;
	private boolean[] activeServers;
	
	
	public MonitorRequest(Server server) {
		sender = server.getSelfId();
		mode = server.getMode();
		ServerState state = server.getState();
		votedFor = state.getVotedFor();
		currentTerm = state.getCurrentTerm();
		commitIndex = state.getCommitIndex();
		lastEntry = state.getLastEntry();
		lastAplied = state.getLastAplied();
		activeServers = new boolean[server.getClusterArray().length];
		
		for (int i = 0; i < activeServers.length; i++) 
			activeServers[i] = server.getClusterFollowBehaviour()[i] != null;
		
	}
}
