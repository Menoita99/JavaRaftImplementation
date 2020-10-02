package com.raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;

import com.raft.models.Address;
import com.raft.models.AppendResponse;
import com.raft.models.Log;
import com.raft.models.VoteResponse;
import com.raft.state.Mode;
import com.raft.state.ServerState;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Server extends LeaderBehavior implements Serializable, FollowerBehavior {

	private static final long serialVersionUID = 1L;

	private ThreadPoolExecutor executor;

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



	private String port,clusterString;

	private Object timeOutInterval;


	public Server() {
		init();
	}





	private void init() {
		//TODO read configuration files
		readIni();
		//Initialise executor 
		//load configurations (config.conf)
		//check for checkpoint
		//load state
		//load rmi registry;
		//Set mode to candidate or follower for first project stage
	}






	private void readIni() {
		port="";clusterString="";
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));

			port = p.getProperty("port");
			//Usa esta string para ir buscar os servidores ao registo rmi  e po-los na lista cluster
			clusterString = p.getProperty("cluster");

			String[] timeOutInterval = p.getProperty("timeOutInterval").trim().split(",");
			maxTimeOut = Integer.parseInt(timeOutInterval[1]);
			minTimeOut = Integer.parseInt(timeOutInterval[0]);
			restartTimer();

		} catch (IOException e) {
			//System.err.println("Config file not found")
			System.err.println("Port : -> " + port + "\nClusterString : -> " + clusterString + "\n timeOutIntervalString : -> "+ timeOutInterval);
			e.printStackTrace();
		}		
	}






	@Override
	public VoteResponse requestVote(long term, Address candidateId, long lastLogIndex, long lastLogTerm)throws RemoteException {
		// TODO Auto-generated method stub
		//Follow paper implementation

		return null;
	}





	/**
	 * Method called by leader to replicate log entries and also used as heartbeat
	 */
	@Override
	public AppendResponse appendEntries(long term, Address leaderId, long prevLogIndex, long prevLogTerm,List<Log> entries, long leaderCommit) throws RemoteException {
		if(entries.isEmpty())//heartBeat
			restartTimer();
		else {
			//sorts entries from log with minus index to the log with the bigger index
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
		return new AppendResponse(state.getCurrentTerm(), state.hasLog(prevLogTerm,prevLogIndex));
	}






	@Override
	public void handleClientRequests() {
		// TODO Auto-generated method stub

	}



	private class ServerTask implements Runnable{
		@Override
		public void run() {
			// TODO Auto-generated method stub

		}
	}


	/**
	 * This method is called by timer when
	 */
	public void startElection() {
		// TODO Auto-generated method stub
		System.out.println("Starting Election");
	}





	private void restartTimer() {
		timer.cancel();
		int timeOut = new Random().nextInt(maxTimeOut-minTimeOut) +minTimeOut;
		timer.schedule(new TimerTask() {
			public void run() {startElection();}
		}, 10, timeOut);
	}







	public static void main(String[] args) {
		List<Integer> of = new ArrayList<>(Arrays.asList(5,2,6,5,3,6,5));
		of.sort((o1,o2) -> o1-o2);
		of.forEach(System.out::println);
	}
}
