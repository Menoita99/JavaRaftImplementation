package com.raft;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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

import com.monitor.MonitorClient;
import com.raft.models.Address;
import com.raft.models.AppendResponse;
import com.raft.models.Entry;
import com.raft.models.ServerResponse;
import com.raft.models.Snapshot;
import com.raft.models.VoteResponse;
import com.raft.state.Mode;
import com.raft.state.ServerState;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Server extends Leader implements Serializable, FollowerBehaviour{

	public static final String CONFIG_INI = "config.ini"; 

	public int heartbeatTimeOut;

	private static final long serialVersionUID = 1L;

	private ExecutorService  executor;
	private ExecutorService  connectorService = Executors.newFixedThreadPool(1);

	private EntryManager entityManager;

	private ServerState state;
	private Mode mode = Mode.FOLLOWER;

	private int timeOutVote;
	private int maxTimeOut;
	private int minTimeOut;

	//Reference to the cluster servers to make RPC (remote procedure call)
	private FollowerBehaviour[] clusterFollowBehaviour;
	private LeaderBehaviour[] clusterLeaderBehaviour;
	private Address[] clusterArray;

	private HeartBeatSender heartBeatSender;

	//This object will notify server when it must change mode to candidate and start an election
	private Timer timer = new Timer();

	private Address leaderId;
	private Address selfId;

	private MonitorClient monitorClient;

	private String root; 




	public Server(String root, boolean monitorMode) throws Exception{
		this.root = root;
		//check if the File exists if so recover the state from the File		
		state = new File(root + "/" + Snapshot.SNAP_FILE_NAME).exists() ? Snapshot.recoverAndInitFromFile(root) : new ServerState(root);

		readIni();
		registServer();
		tryToConnect(true);
		entityManager = new EntryManager(this);
		heartBeatSender = new HeartBeatSender(this);
		heartBeatSender.start(); 
		if(monitorMode) {
			monitorClient = new MonitorClient(this);
			monitorClient.updateStatus();
		}
		restartTimer();
		System.out.println("Server "+selfId+" started");
	}






	/**
	 * Reads configuration file and initialises attributes
	 */
	private void readIni() throws  IOException, AlreadyBoundException {
		Properties p = new Properties();
		p.load(new FileInputStream(root+File.separator+CONFIG_INI));

		selfId = new Address(p.getProperty("ip"), Integer.parseInt(p.getProperty("port")));

		String[] clusterString = p.getProperty("cluster").split(";");

		clusterArray = new Address[clusterString.length];
		clusterLeaderBehaviour = new LeaderBehaviour[clusterString.length];
		clusterFollowBehaviour = new FollowerBehaviour[clusterString.length];

		heartbeatTimeOut = Integer.parseInt(p.getProperty("heartbeatTimeOut"));

		for (int i = 0; i < clusterString.length; i++) {
			String[] splited = clusterString[i].split(":");
			clusterArray[i] = new Address(splited[0], Integer.parseInt(splited[1]));
		}

		executor = Executors.newFixedThreadPool(clusterArray.length);

		String[] timeOutInterval = p.getProperty("timeOutInterval").trim().split(",");
		maxTimeOut = Integer.parseInt(timeOutInterval[1]);
		minTimeOut = Integer.parseInt(timeOutInterval[0]);

		timeOutVote = Integer.parseInt(p.getProperty("timeOutVote"));
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
		Object server = UnicastRemoteObject.exportObject(this, 0);
		System.setProperty( "java.rmi.server.hostname", "127.0.0.1");
		registry.bind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/leader", (LeaderBehaviour)server);
		Naming.rebind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/leader", (LeaderBehaviour)server);
		registry.bind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/follow", (FollowerBehaviour)server);
		Naming.rebind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/follow", (FollowerBehaviour)server);
	}





	/**
	 * Looks for another servers
	 */
	public void tryToConnect(boolean wait) {
		Future<?> submit = connectorService.submit(()->{
			for (int i = 0; i < clusterArray.length; i++) {
				try {
					if(clusterLeaderBehaviour[i] == null)
						clusterLeaderBehaviour[i] =(LeaderBehaviour) Naming.lookup("rmi://" + clusterArray[i].getIpAddress() + ":" + clusterArray[i].getPort() + "/leader");
					if(clusterFollowBehaviour[i] == null)
						clusterFollowBehaviour[i] = (FollowerBehaviour) Naming.lookup("rmi://" + clusterArray[i].getIpAddress() + ":" + clusterArray[i].getPort() + "/follow");
				} catch (MalformedURLException | RemoteException | NotBoundException e) {
					System.err.println("Cloudn't connect to: "+clusterArray[i]);
					if(monitorClient != null)
						monitorClient.updateStatus();
				}
			}
		});
		if(wait)
			try { submit.get();} catch (InterruptedException | ExecutionException e) { e.printStackTrace(); }
	}





	/**
	 * Invoked by candidates to gather votes 
	 * @param term candidate�s term
	 * @param candidateId candidate requesting vote
	 * @param lastEntryIndex index of candidate�s last log entry
	 * @param lastEntryTerm term of candidate�s last log entry
	 * @return VoteResponse
	 * @throws RemoteException extends Remote Interface
	 */
	@Override
	public VoteResponse requestVote(long term, Address candidateId, long lastEntryIndex, long lastEntryTerm)throws RemoteException {
		shouldBecameFollower(term);

		long currentTerm = state.getCurrentTerm();
		if(term<currentTerm) {
			System.out.println(selfId+" NOT "+candidateId+" becuase term to low");
			return new VoteResponse(currentTerm, false);
		} else {
			Address votedFor = state.getVotedFor(); 
			if(votedFor != null && !candidateId.equals(this.state.getVotedFor())) {
				System.out.println(selfId+" NOT "+candidateId+" because i already voted for "+votedFor);
				return new VoteResponse(currentTerm, false);
			}else if(votedFor != null && candidateId.equals(this.state.getVotedFor()) && term == currentTerm) {
				System.out.println(selfId+" granted "+candidateId+" because i already voted for him");
				restartTimer();
				return new VoteResponse(currentTerm, true);
			}else if (votedFor == null && state.getLastAplied().getIndex() <= lastEntryIndex) {
				System.out.println(selfId+" granted "+candidateId+" because last entry is more recent or equals then mine");
				restartTimer();
				state.setVotedFor(candidateId);
				return new VoteResponse(currentTerm, true);
			}
		}


		System.out.println(selfId+" NOT "+candidateId+" idk");
		return new VoteResponse(currentTerm, false);
	}





	/**
	 * Method Invoked by leader to replicate log entries; also used as heartbeat 
	 * @param term leader�s term
	 * @param leaderId so follower can redirect clients
	 * @param prevEntryIndex index of log entry immediately preceding new ones
	 * @param prevEntryTerm term of prevLogIndex entry
	 * @param entries log entries to store (empty for heartbeat; may send more than one for efficiency)
	 * @param leaderCommit leader�s commitIndex
	 * @return AppendResponse
	 * @throws RemoteException extends Remote Interface
	 */
	@Override
	public AppendResponse appendEntries(long term, Address leaderId, long prevEntryIndex, long prevEntryTerm,List<Entry> entries, long leaderCommit) throws RemoteException {
		restartTimer();
		shouldBecameFollower(term);

		if(leaderId.equals(selfId))
			return null;

		if(term<state.getCurrentTerm())
			return new AppendResponse(state.getLastEntry(),state.getCurrentTerm(), false);

		this.leaderId = leaderId;

		boolean hasPreviousLog = state.hasLog(prevEntryTerm,prevEntryIndex);
		//		long start = System.currentTimeMillis();
		if(hasPreviousLog){
			//sorts entries from log with minor index to the log with the bigger index
			entries.sort((o1,o2) -> ((Long)(o1.getIndex()-o2.getIndex())).intValue());
			for (Entry entry : entries) {
				if(entry != null) {
					Entry lastLog = state.getLastEntry();
					if(entry.getIndex() == lastLog.getIndex()+1) {
						state.addEntry(entry);
					}else if(entry.getIndex() == lastLog.getIndex() && entry.getTerm() != lastLog.getTerm() && mode == Mode.FOLLOWER) {
						state.override(entry);
					}else if(entry.getIndex() < lastLog.getIndex()) {
						if(state.getEntry(entry.getIndex()).getTerm() != entry.getTerm()) {
							state.override(entry);
						}
					}
				}
			}
//
//			System.out.println("leaderCommit "+leaderCommit);
//			System.out.println("state commit "+state.getLastEntry().getIndex());
//			System.out.println("commit "+ (leaderCommit > state.getCommitIndex()));
			if(leaderCommit > state.getCommitIndex())
				state.setCommitIndex((Math.min(leaderCommit, state.getLastEntry().getIndex())));
			//			System.out.println("Time to evaluate: "+(System.currentTimeMillis()-start)+" "+entries.size()+" entries");
			//			System.out.println("----------------------------------------");
		}
		if(monitorClient != null && !entries.isEmpty())
			monitorClient.updateStatus();

		return new AppendResponse(state.getLastEntry(),state.getCurrentTerm(), hasPreviousLog);
	}







	/**
	 * Verifies if this server must became a follower
	 */
	private void shouldBecameFollower(long term) {
		if(term > state.getCurrentTerm()) {
			state.setCurrentTerm(term);
			mode = Mode.FOLLOWER;
			if(monitorClient != null)
				monitorClient.updateStatus();
		}
	}





	/**
	 * Method called by Timer when an election must be started
	 */
	public void startElection() {
		if(mode == Mode.FOLLOWER || mode == Mode.CANDIDATE) {
			timer.cancel();
			tryToConnect(false);

			//Transition to candidate state
			mode = Mode.CANDIDATE;

			if(monitorClient != null)
				monitorClient.startedElection();

			this.state.setCurrentTerm(this.state.getCurrentTerm() +1);
			//			this.state.setVotedFor(this.selfId);
			this.leaderId = null;

			System.out.println("[Election] Starting Election "+selfId+" for term "+this.state.getCurrentTerm());

			@SuppressWarnings("unchecked")
			Future<VoteResponse>[] listFuture = (Future<VoteResponse>[]) Array.newInstance(Future.class, clusterArray.length);

			int i=0;
			for(LeaderBehaviour clstr : clusterLeaderBehaviour) {
				if(clstr != null) {
					Future<VoteResponse> resp = executor.submit(()-> clstr.requestVote(this.state.getCurrentTerm(), selfId, this.state.getLastEntry().getIndex(), this.state.getLastEntry().getTerm()));
					listFuture[i]=resp;
				}
				i++;
			}

			int votes = 0;
			i=0;
			for(Future<VoteResponse> ftr : listFuture){
				try {
					if(ftr != null) {
						VoteResponse vote = ftr.get(timeOutVote, TimeUnit.MILLISECONDS);
						if(vote != null && vote.isVoteGranted()) 
							votes++;							
					}
				} catch (Exception e) {
					System.err.println("Counld not send request vote to: "+ clusterArray[i]);
					clusterLeaderBehaviour[i] = null;
					continue;
				}
				i++;
			}	

			System.out.println("[Election] Got "+votes+" votes "+selfId);
			if (votes > clusterArray.length/2 && mode != Mode.FOLLOWER) {
				System.out.println("[Election] I m leader "+selfId);
				mode = Mode.LEADER;
				getLeaderState().reset(this);
				leaderId = selfId;
				heartBeatSender.goOn();

				if(monitorClient != null)
					monitorClient.newLeader();
			}else if((double) votes / (double) clusterArray.length == 0.5)
				startElection();
			else 
				mode = Mode.FOLLOWER;
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
					startElection();
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

			if(monitorClient != null)
				monitorClient.updateStatus();

			try {
				serverResponse.setResponse(state.getInterpreter().getCommandResult(commandID,0));
			} catch (TimeoutException | InterruptedException e) {
				serverResponse.setResponse(e);
				e.printStackTrace();
			}
		}
		if(monitorClient != null)
			monitorClient.commandEval();
		return serverResponse;
	}





	@Override
	public boolean InstallSnapshot(long term, Address leaderId,Snapshot snapshot) {
		restartTimer();
		if(term<state.getCurrentTerm())
			return false;
		this.leaderId = leaderId;

		try {
			System.out.println(snapshot.getState().getCommitIndex());
			snapshot.getState().setRootPath(root);
			state.close();
			snapshot.snap();
			state = Snapshot.recoverAndInitFromFile(root);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		if(monitorClient != null)
			monitorClient.updateStatus();
		return true;
	}






	@Override
	public Address getAddress() throws RemoteException {
		return selfId;
	}
}
