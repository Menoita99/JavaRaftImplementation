package com.raft;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.raft.models.Address;
import com.raft.models.AppendResponse;
import com.raft.models.Log;

public interface FollowerBehavior extends Remote{

	public AppendResponse appendEntries(long term, Address leaderId, long prevLogIndex,long prevLogTerm,List<Log> entries,long leaderCommit) throws RemoteException;

	public long InstallSnapshot(long term, Address leaderId,long lastIncludedIndex,long lastIncludedTerm,long offset, byte[] data, boolean done) throws RemoteException;
}
