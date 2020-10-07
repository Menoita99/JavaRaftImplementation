package com.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Properties;

import com.raft.LeaderBehaviour;
import com.raft.Server;
import com.raft.models.ServerResponse;

public class Client {

	/*
	 * 1-cliente liga e conecta-se a um server random se nao for lider o follower
	 * rencaminha po lider e este faz o pedido de novo se o lider crasha ele volta a
	 * escolher um server random
	 * 
	 * cada comando do cliente tem o mesmo id
	 **/

	private String clusterMembers ;
	private int tryCount = -1;
	private String clusterMembersVector[];
	private LeaderBehaviour look_up;
	private ArrayList<String> logsList;
	public Client() {
		readIni();
		connectToServer();
	}


	private void readIni() {
		logsList = new ArrayList<>();
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));

			clusterMembers = p.getProperty("cluster");

		} catch (IOException e) {
			e.printStackTrace();
		}
		clusterMembersVector = clusterMembers.split(";");
	}


	public String generateFullLog(String log) {
		String logID = "clientID:";
		try {
			logID += Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logID+="null";
			e.printStackTrace();
		}

		logID += ":"+System.currentTimeMillis();
		String generatedLog = logID + "-" + log;
		logsList.add(generatedLog);
		return generatedLog;
	}


	public void connectToServer() {
		if(tryCount==clusterMembersVector.length-1)
			return;
		
		//Suggestion use a for loop instead of a recursive method
		tryCount++;
		String ip = clusterMembersVector[tryCount].split(":")[0];
		String port = clusterMembersVector[tryCount].split(":")[1];
		System.out.println("Request ->"+ip + ":"+port);

		try {

			look_up = (LeaderBehaviour) Naming.lookup("rmi://" + ip + ":" + port + "/server");

			ServerResponse response = look_up.request(generateFullLog("abcdtest"));
			// If the Object of the ServerResponse instance is null, that means it received
			// the Address of the leader. Try reconnect to leader
			System.out.println(response.toString());
			System.out.println(response.getLeader());
			if (response.getResponse()==null) {
				ip = response.getLeader().getIpAddress();
				port = String.valueOf(response.getLeader().getPort());
				//look_up = (Server) Naming.lookup("rmi://" + ip + ":" + port + "/server");

				//response = look_up.request(generateFullLog("abcdtest"));
			}


		} catch (NotBoundException | MalformedURLException | RemoteException e) {
			System.out.println("This cluster member is offline");
			connectToServer();
		}
	}
}
