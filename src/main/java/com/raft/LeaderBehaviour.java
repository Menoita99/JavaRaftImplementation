package com.raft;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.raft.models.Address;
import com.raft.models.ServerResponse;
import com.raft.models.VoteResponse;
import com.raft.state.LeaderState;

import lombok.Getter;
import lombok.Setter;

/**
 * Class that represents the Leader Behaviour. 
 * This class also has the leader State object
 * @author RuiMenoita
 */
@Getter
@Setter
public abstract class LeaderBehaviour implements Remote{
	private LeaderState leaderState;
	
	/**
	 * Invoked by candidates to gather votes 
	 * @param term candidate’s term
	 * @param candidateId candidate requesting vote
	 * @param lastLogIndex index of candidate’s last log entry
	 * @param lastLogTerm term of candidate’s last log entry
	 * @return VoteResponse
	 * @throws RemoteException extends Remote Interface
	 */
	public abstract VoteResponse requestVote(long term,Address candidateId, long lastLogIndex, long lastLogTerm) throws RemoteException;

	
	/**
	 * Method invoked by client to request the execution of the given command
	 * and processed by server. Depending of the server Mode different responses must be obtained
	 * @param command command given by client to be executed
	 * @return ServerResponse
	 * @throws RemoteException extends Remote Interface
	 */
	public abstract ServerResponse request(String command) throws RemoteException;
}
