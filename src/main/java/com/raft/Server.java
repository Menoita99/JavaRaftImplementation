package com.raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.raft.models.Address;
import com.raft.models.AppendResponse;
import com.raft.models.Log;
import com.raft.models.ServerResponse;
import com.raft.models.VoteResponse;
import com.raft.state.Mode;
import com.raft.state.ServerState;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Server extends Leader implements Serializable, FollowerBehaviour{

	private static final long serialVersionUID = 1L;

	private ExecutorService  executor;

	//Server State
	private ServerState state;
	//Server mode (FOLLOWER,CANDIDATE,LEADER)
	private Mode mode = Mode.FOLLOWER;
	//Randomly picked timeOut in milliseconds to start an election
	private int maxTimeOut;
	private int minTimeOut;

	//Reference to the cluster servers to make RPC (remote procedure call)
	private LinkedList<Server> cluster = new LinkedList<>();

	//Machine state
	//This map contains the state of variables manipulated by client commands
	private HashMap<String,Object> objects = new HashMap<>();

	//This object will notify server when it must change mode to candidate and start an election
	private Timer timer = new Timer();

	private Address leaderId;
	
	private Address selfId;



	public Server(){
		try {
			this.state = new ServerState();
			readIni();
		} catch (IOException | AlreadyBoundException e) {
			e.printStackTrace();
		}
		//load configurations (config.conf)
		//check for checkpoint
		//load rmi registry;
	}




	//temp para testar, alterar depois o port abaixo 87: int port = .... e apagar l79
	private int port;
	/**
	 * Reads configuration file and initialises attributes
	 */
	private void readIni() throws  IOException, AlreadyBoundException {
		Properties p = new Properties();
		p.load(new FileInputStream("src/main/resources/config.ini"));

		port = Integer.parseInt(p.getProperty("port"));
		String clusterString = p.getProperty("cluster");
		executor = Executors.newFixedThreadPool(clusterString.split(";").length);

		String[] timeOutInterval = p.getProperty("timeOutInterval").trim().split(",");
		maxTimeOut = Integer.parseInt(timeOutInterval[1]);
		minTimeOut = Integer.parseInt(timeOutInterval[0]);
		System.out.println("Server "+p.getProperty("ip")+":"+port);
		
		leaderId = new Address(p.getProperty("liderIp"), Integer.parseInt(p.getProperty("liderPort")));
		System.out.println(leaderId);
		
		Registry registry = LocateRegistry.createRegistry(port);
		LeaderBehaviour object = (LeaderBehaviour) UnicastRemoteObject.exportObject(this, 0);
		registry.bind("rmi://"+p.getProperty("ip")+":"+port+"/server", object);
		Naming.rebind("rmi://"+p.getProperty("ip")+":"+ port+"/server", object);
		
		
		restartTimer();
	}





	/**
	 * Invoked by candidates to gather votes 
	 * @param term candidate�s term
	 * @param candidateId candidate requesting vote
	 * @param lastLogIndex index of candidate�s last log entry
	 * @param lastLogTerm term of candidate�s last log entry
	 * @return VoteResponse
	 * @throws RemoteException extends Remote Interface
	 */
	@Override
	public VoteResponse requestVote(long term, Address candidateId, long lastLogIndex, long lastLogTerm)throws RemoteException {
		shouldBecameFollower(term);
		// TODO CHECK FOR IMPLEMENTATION
		boolean voteGranted = false;
		VoteResponse resposta = new VoteResponse(this.state.getCurrentTerm(), voteGranted);

		if (term < this.state.getCurrentTerm()) {
			return resposta;
		}else {
			if (this.state.getVotedFor() == null || this.state.getVotedFor().equals(candidateId)
					&& this.state.getCommitIndex() == lastLogIndex && this.state.getCurrentTerm() == lastLogTerm) {
				resposta.setVoteGranted(true);
			}
		}

		return resposta;
	}





	/**
	 * Method Invoked by leader to replicate log entries; also used as heartbeat 
	 * @param term leader�s term
	 * @param leaderId so follower can redirect clients
	 * @param prevLogIndex index of log entry immediately preceding new ones
	 * @param prevLogTerm term of prevLogIndex entry
	 * @param entries log entries to store (empty for heartbeat; may send more than one for efficiency)
	 * @param leaderCommit leader�s commitIndex
	 * @return AppendResponse
	 * @throws RemoteException extends Remote Interface
	 */
	@Override
	public AppendResponse appendEntries(long term, Address leaderId, long prevLogIndex, long prevLogTerm,List<Log> entries, long leaderCommit) throws RemoteException {
		this.leaderId = leaderId;
		shouldBecameFollower(term);

		boolean hasPreviousLog = state.hasLog(prevLogTerm,prevLogIndex);

		if(entries.isEmpty())//heartBeat
			restartTimer();
		else if(hasPreviousLog){
			//sorts entries from log with minor index to the log with the bigger index
			entries.sort((o1,o2) -> ((Long)(o1.getIndex()-o2.getIndex())).intValue());

			for (Log log : entries) {
				Log lastLog = state.getLastLog();
				if((log.getIndex() == lastLog.getIndex() && log.getTerm() != lastLog.getTerm()) || log.getIndex()<lastLog.getIndex())
					if(mode == Mode.FOLLOWER)
						state.override(log);
				if(log.getIndex()>lastLog.getIndex())
					state.appendLog(log);
			}

			if(leaderCommit > state.getCommitIndex())
				state.setCommitIndex(Math.min(leaderCommit, state.getLastLog().getIndex()));
		}
		return new AppendResponse(state.getCurrentTerm(), hasPreviousLog);
	}







	/**
	 * Verifies if this server must became a follower
	 */
	private void shouldBecameFollower(long term) {
		if(mode == Mode.FOLLOWER)
			return;
		if(term > state.getCurrentTerm()) {
			mode = Mode.FOLLOWER;
			restartTimer();
		}
	}





	/**
	 * Method called by Timer when an election must be started
	 */
	public void startElection() {
		long currTerm = this.state.getCurrentTerm();
		if(mode == Mode.FOLLOWER) {
			System.out.println("Starting Election");
			//Increments its current term
			currTerm++;
			//Transitions to the candidate state
			mode = Mode.CANDIDATE;
			//Votes for itself 
			this.state.setVotedFor(this.selfId);
			//Send RequestVote to every other Server
			for(Server clstr : cluster) {
				try {
					clstr.requestVote(currTerm, selfId, this.state.getLastLog().getIndex(), this.state.getLastLog().getTerm());
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}







	/**
	 * Restarts the timer that will trigger an election with a random value 
	 * between minTimeOut and maxTimeOut
	 */
	private void restartTimer() {
		timer.cancel();
		timer = new Timer();
		int timeOut = new Random().nextInt(maxTimeOut-minTimeOut) +minTimeOut;
		timer.schedule(new TimerTask() {
			public void run() {System.out.println("Start Election in "+timeOut+"ms");startElection();}
		}, timeOut);
	}






	/**
	 * Method invoked by client to request the execution of the given command
	 * if current server is a follower it returns an empty response only containing leader's address
	 * if current server is a candidate //TODO
	 * if current server is a leader it executes {@link leaderResponse}
	 * @param command command given by client to be executed
	 * @return ServerResponse
	 */
	@Override
	public ServerResponse request(String string) throws RemoteException{

		//TEMP
		if(port == 1000)
			this.mode=Mode.LEADER;
		else {
			this.mode=Mode.FOLLOWER;
		}
		//TEMP

		switch (mode) {
		case FOLLOWER: {
			return requestLeader();
		}
		case CANDIDATE:{
			//TODO
			return null;
		}case LEADER:{
			return leaderResponse(string);
		}
		}
		return null;
	}





	/**
	 * This method works as described by https://raft.github.io/raft.pdf paper "Rules for Servers" -> leader part
	 * This method also use Java futures for multi-threading
	 * @param string client command
	 * @return ServerResponse
	 */
	private ServerResponse leaderResponse(String string) {
		//TODO
		System.out.println("received "+string);
		//dar add ao comando (por decidir)
		ServerResponse serverResponse = new ServerResponse(null,  string + ";test123");
		return serverResponse;
	}





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
	@Override
	public long InstallSnapshot(long term, Address leaderId, long lastIncludedIndex, long lastIncludedTerm, long offset,byte[] data, boolean done) {
		this.leaderId = leaderId;
		shouldBecameFollower(term);
		// TODO Auto-generated method stub
		return 0;
	}






	@Override
	public ServerResponse requestLeader() throws RemoteException {
		// TODO Auto-generated method stub
		ServerResponse serverResponse = new ServerResponse(leaderId,"not today");
		return serverResponse;	
	}
}
