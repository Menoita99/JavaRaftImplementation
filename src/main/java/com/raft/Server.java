package com.raft;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
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
	private Mode mode;
	//Randomly picked timeOut in milliseconds to start an election
	private long timeOut;
	
	//Reference to the cluster servers to make RPC (remote procedure call)
	private LinkedList<Server> cluster = new LinkedList<>();
	
	//Machine state
	//This map contains the state of variables manipulated by client commands
	private HashMap<String,Object> objects = new HashMap<>();

	private String port,clusterString,timeOutIntervalString;

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
	}


	
	
	
	
	private void readIni() {
		port="";clusterString="";timeOutIntervalString="";
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));
			
			port = p.getProperty("port");
			clusterString = p.getProperty("cluster");
			timeOutIntervalString = p.getProperty("timeOutInterval");
		} catch (IOException e) {
			System.err.println("Port : -> " + port + "\nClusterString : -> " + clusterString + "\n timeOutIntervalString : -> " +timeOutIntervalString);
			e.printStackTrace();
		}		
	}





	@Override
	public VoteResponse requestVote(long term, Address candidateId, long lastLogIndex, long lastLogTerm)throws RemoteException {
		// TODO Auto-generated method stub
		//User for multi-threading FUTURES
		//Follow paper implementation
		return null;
	}


	
	
	
	
	@Override
	public AppendResponse appendEntries(long term, Address leaderId, long prevLogIndex, long prevLogTerm,List<Log> entries, long leaderCommit) throws RemoteException {
		// TODO Auto-generated method stub
		//User for multi-threading FUTURES
		//Follow paper implementation
		return null;
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
}
