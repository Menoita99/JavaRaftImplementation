package com.raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.raft.models.Address;
import com.raft.models.AppendResponse;
import com.raft.models.Entry;
import com.raft.models.ServerResponse;
import com.raft.models.VoteResponse;
import com.raft.state.Mode;
import com.raft.state.ServerState;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Server extends Leader implements Serializable, FollowerBehaviour{

	public static final int WAIT_FOR_RESPONSE_TIME_OUT = 250;
	public static final int HEARTBEAT_TIME_OUT = 100;

	private static final long serialVersionUID = 1L;

	private ExecutorService  executor;

	private EntryManager entityManager;

	//Server State
	private ServerState state;
	//Server mode (FOLLOWER,CANDIDATE,LEADER)
	private Mode mode = Mode.FOLLOWER;
	//Randomly picked timeOut in milliseconds to start an election
	private int maxTimeOut;
	private int minTimeOut;

	//Reference to the cluster servers to make RPC (remote procedure call)
	private ArrayList<FollowerBehaviour> clusterFollow = new ArrayList<>();
	private ArrayList<LeaderBehaviour> clusterLeader = new ArrayList<>();
	private Address[] clusterArray;

	private HeartBeatSender heartBeatSender;

	//This object will notify server when it must change mode to candidate and start an election
	private Timer timer = new Timer();

	private Address leaderId;
	private Address selfId;


	/**
	 * This constructor must only be used to test
	 */
	public Server(Properties p) throws IOException, AlreadyBoundException{
		state = new ServerState();
		readIni(p);
		registServer();
		tryToConnect();
		entityManager = new EntryManager(this);
		heartBeatSender = new HeartBeatSender(this);
		heartBeatSender.start();
		restartTimer();
	}






	public Server() throws IOException, AlreadyBoundException{
		state = new ServerState();
		readIni(null);
		registServer();
		tryToConnect();
		entityManager = new EntryManager(this);
		heartBeatSender = new HeartBeatSender(this);
		heartBeatSender.start();
		restartTimer();
	}






	/**
	 * Reads configuration file and initialises attributes
	 */
	private void readIni(Properties p) throws  IOException, AlreadyBoundException {
		if(p == null) { 
			p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));
		}
		selfId = new Address(p.getProperty("ip"), Integer.parseInt(p.getProperty("port")));
		System.out.println(selfId);

		String[] clusterString = p.getProperty("cluster").split(";");
		clusterArray = new Address[clusterString.length];

		for (int i = 0; i < clusterString.length; i++) {
			clusterLeader.add(null);
			clusterFollow.add(null);
		}

		for (int i = 0; i < clusterString.length; i++) {
			String[] splited = clusterString[i].split(":");
			clusterArray[i] = new Address(splited[0], Integer.parseInt(splited[1]));
		}

		executor = Executors.newFixedThreadPool(clusterArray.length);

		String[] timeOutInterval = p.getProperty("timeOutInterval").trim().split(",");
		maxTimeOut = Integer.parseInt(timeOutInterval[1]);
		minTimeOut = Integer.parseInt(timeOutInterval[0]);
	}





	/**
	 * Makes server Online
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 * @throws AccessException
	 * @throws MalformedURLException
	 */
	private void registServer() throws RemoteException, AlreadyBoundException, AccessException, MalformedURLException {
		Registry registry = LocateRegistry.createRegistry(selfId.getPort());
		LeaderBehaviour server = (LeaderBehaviour) UnicastRemoteObject.exportObject(this, 0);
		System.setProperty( "java.rmi.server.hostname", "127.0.0.1");
		registry.bind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/leader", server);
		Naming.rebind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/leader", server);
		registry.bind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/follow", (FollowerBehaviour)server);
		Naming.rebind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/follow", (FollowerBehaviour)server);
	}





	/**
	 * Looks for another servers
	 */
	public void tryToConnect() {
		for (int i = 0; i < clusterArray.length; i++) {

			if( clusterFollow.size() < i || clusterFollow.get(i) == null ) {
				try {
					clusterFollow.remove(i);
					clusterFollow.add(i,(FollowerBehaviour) Naming.lookup("rmi://" + clusterArray[i].getIpAddress() + ":" + clusterArray[i].getPort() + "/follow"));
				} catch (MalformedURLException | RemoteException | NotBoundException e) {
					clusterFollow.add(i,null);
				}
			}

			if( clusterLeader.size() < i || clusterLeader.get(i) == null ) {
				try {
					clusterLeader.remove(i);
					clusterLeader.add(i,(LeaderBehaviour) Naming.lookup("rmi://" + clusterArray[i].getIpAddress() + ":" + clusterArray[i].getPort() + "/leader"));
				} catch (MalformedURLException | RemoteException | NotBoundException e) {
					clusterLeader.add(i,null);
				}
			}

		}
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
			if (this.state.getVotedFor() == null || this.state.getVotedFor().equals(candidateId)&& this.state.getCommitIndex() == lastLogIndex && this.state.getCurrentTerm() == lastLogTerm) {
				resposta.setVoteGranted(true);
				state.setVotedFor(candidateId);
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
	public AppendResponse appendEntries(long term, Address leaderId, long prevLogIndex, long prevLogTerm,List<Entry> entries, long leaderCommit) throws RemoteException {
			restartTimer();
			if(leaderId.equals(selfId))
				return null;
			
			shouldBecameFollower(term);
			this.leaderId = leaderId;

			boolean hasPreviousLog = state.hasLog(prevLogTerm,prevLogIndex);

			if(hasPreviousLog){
				//sorts entries from log with minor index to the log with the bigger index
				entries.sort((o1,o2) -> ((Long)(o1.getIndex()-o2.getIndex())).intValue());

				for (Entry entry : entries) {
					if(entry != null) {
						Entry lastLog = state.getLastEntry();
						if((entry.getIndex() == lastLog.getIndex() && entry.getTerm() != lastLog.getTerm()) || entry.getIndex()<lastLog.getIndex())
							if(mode == Mode.FOLLOWER)
								state.override(entry);
						if(entry.getIndex()>lastLog.getIndex())
							state.addEntry(entry);
					}
				}

				if(leaderCommit > state.getCommitIndex())
					state.setCommitIndex((Math.min(leaderCommit, state.getLastEntry().getIndex())));
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
		if(mode == Mode.FOLLOWER || mode == Mode.CANDIDATE) {
			timer.cancel();
			tryToConnect();
			System.out.println("Starting Election");

			//Transition to candidate state
			mode = Mode.CANDIDATE;
			this.state.setCurrentTerm(this.state.getCurrentTerm() +1);
			this.state.setVotedFor(this.selfId);

			//Send RequestVote to every other Server
			ArrayList<Future<VoteResponse>> listFuture = new ArrayList<>();
			for(LeaderBehaviour clstr : clusterLeader) {
				if(clstr != null) {
					Future<VoteResponse> resp = executor.submit(()-> clstr.requestVote(this.state.getCurrentTerm(), selfId, this.state.getLastEntry().getIndex(), this.state.getLastEntry().getTerm()));
					listFuture.add(resp);
				}
			}
			int votes = 1;

			for(Future<VoteResponse> ftr : listFuture){
				try {
					VoteResponse vote = ftr.get(WAIT_FOR_RESPONSE_TIME_OUT, TimeUnit.MILLISECONDS);
					if(vote.isVoteGranted())
						votes++;								
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					e.printStackTrace();
					continue;
				}
			}	

			if (votes > clusterArray.length/2) {
				mode = Mode.LEADER;
				getLeaderState().reset(this);
				leaderId = selfId;
				heartBeatSender.goOn();
			}else if((double) votes / (double) clusterArray.length == 0.5){
				startElection();
			}
			restartTimer();
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
			public void run() {
				//				startElection();
			}
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
	public ServerResponse execute(String string, String commandID) throws RemoteException{
		switch (mode) {
		case FOLLOWER: 
			return new ServerResponse(leaderId, null);
		case CANDIDATE:
			return new ServerResponse(null, null);
		case LEADER:
			return leaderResponse(string, commandID);
		default:
			throw new IllegalArgumentException("Unexpected value: " + mode);
		}
	}





	/**
	 * This method works as described by https://raft.github.io/raft.pdf paper "Rules for Servers" -> leader part
	 * This method also use Java futures for multi-threading
	 * @param string client command
	 * @return ServerResponse
	 */
	private ServerResponse leaderResponse(String command, String commandID) {
		ServerResponse serverResponse;

		if(command==null || command.isBlank())
			serverResponse = new ServerResponse(selfId,  null);
		else {
			serverResponse = new ServerResponse(selfId,  command);

			String clientIP = commandID.split(":")[0];
			//if there already is an entry from the client requesting command, and this command has already been executed, directly sends response
			//which had been stored when it was originally executed
			for (String key : state.getInterpreter().getOperationsMap().keySet()) {
				if(key.equals(clientIP)) {
					if (state.getInterpreter().getOperationsMap().get(key).getOperationID().equals(commandID)) {
						serverResponse.setResponse(state.getInterpreter().getOperationsMap().get(key).getResponse());
						return serverResponse;
					}
				}
			}

			Entry entry = state.createEntry(command, commandID);
			entityManager.submitEntry(entry);
			try {
				serverResponse.setResponse(state.getInterpreter().getCommandResult(commandID, 999999999));
			} catch (TimeoutException | InterruptedException e) {
				serverResponse.setResponse(e);
			}
		}
		System.out.println(serverResponse);
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


}
