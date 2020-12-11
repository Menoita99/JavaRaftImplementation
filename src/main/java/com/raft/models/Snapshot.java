package com.raft.models;

import com.raft.state.ServerState;

public class Snapshot {

	private long lastIncludedIndex;
	private long lastIncludedTerm;
	private ServerState serverState;
	
	public Snapshot(ServerState serverState) {
		this.serverState = serverState;
	}
	
	public void snap() {
		lastIncludedTerm = serverState.getCurrentTerm();
		lastIncludedIndex = serverState.getCommitIndex();
		
		//Nao sei o que é para guardar como state machine state
	}
	
	public void save() {
		
	}
	
}
