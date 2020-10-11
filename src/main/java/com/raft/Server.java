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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.raft.models.Log;
import com.raft.models.Operation;
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

	private Map<String, Operation> operationsMap = new HashMap<String, Operation>();


	/**
	 * This constructor must only be used to test
	 */
	public Server(Properties p) throws IOException, AlreadyBoundException{
		state = new ServerState();
		readIni(p);
		registServer();
		tryToConnect();
		heartBeatSender = new HeartBeatSender(this);
		heartBeatSender.start();
		restartTimer();
	}






	public Server() throws IOException, AlreadyBoundException{
		state = new ServerState();
		readIni(null);
		registServer();
		tryToConnect();
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

		leaderId = new Address(p.getProperty("liderIp"), Integer.parseInt(p.getProperty("liderPort")));
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
	private void tryToConnect() {
		for (int i = 0; i < clusterArray.length; i++) {
			try {
				if( clusterFollow.size() <= i || clusterFollow.get(i) == null )
					clusterFollow.add(i,(FollowerBehaviour) Naming.lookup("rmi://" + clusterArray[i].getIpAddress() + ":" + clusterArray[i].getPort() + "/follow"));
				if( clusterLeader.size() <= i || clusterLeader.get(i) == null )
					clusterLeader.add(i,(LeaderBehaviour) Naming.lookup("rmi://" + clusterArray[i].getIpAddress() + ":" + clusterArray[i].getPort() + "/leader"));
			} catch (MalformedURLException | RemoteException | NotBoundException e) {
				e.printStackTrace();
				continue;
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
		restartTimer();
		this.leaderId = leaderId;
		shouldBecameFollower(term);

		boolean hasPreviousLog = state.hasLog(prevLogTerm,prevLogIndex);
		//		boolean hasPreviousLog = prevLogIndex == state.getLastLog().getIndex() && prevLogTerm == state.getLastLog().getTerm();

		if(hasPreviousLog){
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
		if(mode == Mode.FOLLOWER || mode == Mode.CANDIDATE) {
			System.out.println("Starting Election");

			//Transition to candidate state
			mode = Mode.CANDIDATE;
			this.state.setCurrentTerm(this.state.getCurrentTerm() +1);
			this.state.setVotedFor(this.selfId);
			restartTimer();

			//Send RequestVote to every other Server
			ArrayList<Future<VoteResponse>> listFuture = new ArrayList<>();
			for(LeaderBehaviour clstr : clusterLeader) {
				Future<VoteResponse> resp = executor.submit(()-> clstr.requestVote(this.state.getCurrentTerm(), selfId, this.state.getLastLog().getIndex(), this.state.getLastLog().getTerm()));
				listFuture.add(resp);
			}
			int votes = 0;

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
				restartTimer();
			}else if((double) votes / (double) clusterArray.length == 0.5) {
				startElection();
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
			public void run() {
				System.out.println("Start Election in "+timeOut+"ms");
				//startElection();
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
		//TEMP
		if(selfId.getPort() == 1000) {
			this.mode=Mode.LEADER;
			getLeaderState().reset(this);
		}else {
			this.mode=Mode.FOLLOWER;
		}
		//TEMP

		switch (mode) {
		case FOLLOWER: 
			System.out.println("Im follower and Received request from client. Sent leader address");
			return new ServerResponse(leaderId, null);
		case CANDIDATE:
			//TODO
			return null;
		case LEADER:
			System.out.println("Im leader and Received request from client");
			return leaderResponse(string, commandID);

		}
		return null;
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
			serverResponse = new ServerResponse(leaderId,  null);
		else {
			serverResponse = new ServerResponse(leaderId,  command);

			//commandID format is 'clientIP:timestamp'
			String clientIP = commandID.split(":")[0];

			//if there already is an entry from the client requesting command, and this command has already been executed, directly sends response
			//which had been stored when it was originally executed
			for (Map.Entry<String, Operation> entry : operationsMap.entrySet()) {
				if(entry.getKey()==clientIP) {
					if (entry.getValue().getOperationID()==commandID) {
						serverResponse.setResponse(entry.getValue().getResponse());
						return serverResponse;
					}
				}
			}

			try {
				//executes command and stores the response on the serverResponse and also on the map associated with the clientIP
				Object response = state.getInterpreter().execute(command);
				serverResponse.setResponse(response);
				if (operationsMap.containsKey(clientIP)) {
					System.out.println("Updated entry on map for client: " + clientIP);
				}
				else {
					System.out.println("Created new entry on map for client: " + clientIP);
				}
				operationsMap.put(clientIP, new Operation(commandID, response));
				System.out.println(operationsMap.toString());

			}catch (Exception e) {
				serverResponse.setResponse(e);
				e.printStackTrace();
			}
			Log log = new Log(state.getLastLog().getIndex()+1, state.getCurrentTerm(), command);
			sendAppendEntriesRequest(log);


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






	public boolean sendAppendEntriesRequest(Log entry) {
		try {
			if(mode == Mode.LEADER) {
				state.getLock().lock();
				tryToConnect();
				long term = state.getCurrentTerm();
				long prevLogIndex = state.getLastLog().getIndex();
				long prevLogTerm = state.getLastLog().getTerm();
				long leaderCommit = state.getCommitIndex();

				List<Future<AppendResponse>> futures = new ArrayList<>(clusterFollow.size());
				for (int i = 0; i < clusterFollow.size(); i++) {
					FollowerBehaviour follower = clusterFollow.get(i);
					if(follower != null) {
						List<Log> entries = new ArrayList<>();
						entries.add(entry);
						futures.add(i,executor.submit(()->follower.appendEntries(term, selfId, prevLogIndex, prevLogTerm,entries, leaderCommit)));
						}
					}

				List<AppendResponse> responses = new ArrayList<>(clusterFollow.size());
				for (int i = 0; i < futures.size(); i++) {
					try {
						Future<AppendResponse> future = futures.get(i);
						if(future != null)
							responses.add(i,future.get(WAIT_FOR_RESPONSE_TIME_OUT, TimeUnit.MILLISECONDS));
					} catch (InterruptedException | ExecutionException e) {
						e.printStackTrace();
					} catch (TimeoutException e) {
						//TODO
						if(entry != null)
							System.out.println("TODO RETRY UNTIL IT WORK");
					}
				}

				int commited = 0;
				for (int i = 0; i < responses.size(); i++) {
					AppendResponse response = responses.get(i);
					if(response != null) {
						long appendedIndex = getLeaderState().getNextIndex().get(clusterArray[i]);
						if(response.isSuccess()) {
							getLeaderState().getNextIndex().put(clusterArray[i], appendedIndex+1);
							getLeaderState().getMatchIndex().put(clusterArray[i], appendedIndex);
							commited++;
						}else {
							getLeaderState().getNextIndex().put(clusterArray[i], appendedIndex-1 < 0 ? 0 : appendedIndex-1);
						}
					}
				}

				if (commited > clusterArray.length/2 && entry != null) {
					state.setCommitIndex(entry.getIndex());
					//				state.appendLog(entry);
					return true;
				}
			}
			return false;
		}finally {
			state.getLock().unlock();
		}
	}
}
