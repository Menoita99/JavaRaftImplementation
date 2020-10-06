package com.raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
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
import com.raft.models.Log;
import com.raft.models.ServerResponse;
import com.raft.models.VoteResponse;
import com.raft.state.Mode;
import com.raft.state.ServerState;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Server extends LeaderBehavior implements Serializable, FollowerBehavior {

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



	public Server() {
		init();
	}



	private void init() {
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



	private void readIni() throws  IOException, AlreadyBoundException {
		Properties p = new Properties();
		p.load(new FileInputStream("src/main/resources/config.ini"));

		int port = Integer.parseInt(p.getProperty("port"));
		String clusterString = p.getProperty("cluster");
		executor = Executors.newFixedThreadPool(clusterString.split(";").length);

		String[] timeOutInterval = p.getProperty("timeOutInterval").trim().split(",");
		maxTimeOut = Integer.parseInt(timeOutInterval[1]);
		minTimeOut = Integer.parseInt(timeOutInterval[0]);

		//Regist this server 
		Registry registry = LocateRegistry.createRegistry(port);
		registry.bind("rmi://"+p.getProperty("ip")+":"+port+"/server", UnicastRemoteObject.exportObject(this, 0));
		restartTimer();
	}




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
	 * Method called by leader to replicate log entries and also used as heartbeat
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





	private void shouldBecameFollower(long term) {
		if(mode == Mode.FOLLOWER)
			return;
		if(term > state.getCurrentTerm()) {
			mode = Mode.FOLLOWER;
			restartTimer();
		}
	}






	public void startElection() {
		// TODO Auto-generated method stub
		if(mode == Mode.FOLLOWER) {
			System.out.println("Starting Election");
		}
	}






	private void restartTimer() {
		timer.cancel();
		timer = new Timer();
		int timeOut = new Random().nextInt(maxTimeOut-minTimeOut) +minTimeOut;
		timer.schedule(new TimerTask() {
			public void run() {System.out.println("Start Election in "+timeOut+"ms");startElection();}
		}, timeOut);
	}






	@Override
	public ServerResponse request(String string) {
		switch (mode) {
			case  FOLLOWER: {
				System.out.println("é bem ze89");
				return new ServerResponse(leaderId, null);
			}
			case CANDIDATE:{
				//TODO
				return null;
			}case LEADER:{
	
				List<Future<AppendResponse>> futures = new ArrayList<>();
				for (Server server : cluster) 
					futures.add(executor.submit(() -> server.appendEntries(0, null, 0, 0, null, 0)));
				List<AppendResponse> responses = new ArrayList<>();
				for (Future<AppendResponse> future : futures) {
					try {
						responses.add(future.get(200, TimeUnit.MILLISECONDS));
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						System.err.println("Server failed to respond");
						continue;
					}
				}
				
				return new ServerResponse(null, "abcdtest");
				
			}
		}
		return null;
	}



	@Override
	public long InstallSnapshot(long term, Address leaderId, long lastIncludedIndex, long lastIncludedTerm, long offset,byte[] data, boolean done) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	
	




	public static void main(String[] args) {
		try {
			Naming.rebind("rmi://" + "127.0.0.1" + ":"+ 1000 + "/server", new Server());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
