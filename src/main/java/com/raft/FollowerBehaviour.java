package com.raft;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.raft.models.Address;
import com.raft.models.AppendResponse;
import com.raft.models.Log;
import com.raft.models.ServerResponse;
/**
 * Class the represents the behaviour of a server when it is in Follower mode
 * @author RuiMenoita
 *
 */
public interface FollowerBehaviour extends Remote{

	/**
	 * Method Invoked by leader to replicate log entries; also used asheartbeat 
	 * @param term leader�s term
	 * @param leaderId so follower can redirect clients
	 * @param prevLogIndex index of log entry immediately preceding new ones
	 * @param prevLogTerm term of prevLogIndex entry
	 * @param entries log entries to store (empty for heartbeat; may send more than one for efficiency)
	 * @param leaderCommit leader�s commitIndex
	 * @return AppendResponse
	 * @throws RemoteException extends Remote Interface
	 */
	public AppendResponse appendEntries(long term, Address leaderId, long prevLogIndex,long prevLogTerm,List<Log> entries,long leaderCommit) throws RemoteException;

	
	
	/*
	 *This method may chance it's parameters since using Java RMI we don't  need chunks and data because Java RMI handles
	 *low level connection issues for us 
	 */
	/**
	 * Method Invoked by leader to send chunks of a snapshot to a follower. Leaders always send chunks in order.
	 * @param term leader�s term
	 * @param leaderId so follower can redirect clients
	 * @param lastIncludedIndex the snapshot replaces all entries up through and including this index
	 * @param lastIncludedTerm term of lastIncludedIndex
	 * @param offset byte offset where chunk is positioned in the snapshot file
	 * @param data raw bytes of the snapshot chunk, starting at offset
	 * @param done true if this is the last chunk
	 * @return currentTerm, for leader to update itself
	 * @throws RemoteException extends Remote Interface
	 */
	public long InstallSnapshot(long term, Address leaderId,long lastIncludedIndex,long lastIncludedTerm,long offset, byte[] data, boolean done) throws RemoteException;

	

}
