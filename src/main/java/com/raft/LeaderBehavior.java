package com.raft;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.raft.models.Address;
import com.raft.models.ServerResponse;
import com.raft.models.VoteResponse;
import com.raft.state.LeaderState;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class LeaderBehavior implements Remote{
	private LeaderState leaderState;
	
	public abstract VoteResponse requestVote(long term,Address candidateId, long lastLogIndex, long lastLogTerm) throws RemoteException;

	public abstract ServerResponse request(String string) throws RemoteException;
}
