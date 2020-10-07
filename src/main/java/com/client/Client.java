package com.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

import com.raft.LeaderBehaviour;
import com.raft.Server;
import com.raft.models.Address;
import com.raft.models.ServerResponse;

import lombok.Data;

@Data
public class Client {

	/*
	 * 1-cliente liga e conecta-se a um server random se nao for lider o follower
	 * rencaminha po lider e este faz o pedido de novo se o lider crasha ele volta a
	 * escolher um server random
	 * 
	 * cada comando do cliente tem o mesmo id
	 **/
	private Address address;
	
	private String clusterMembers ;
	private int tryCount = -1;
	private String clusterMembersVector[];
	private LeaderBehaviour look_up;

	private String leaderPort;
	private String leaderIp;
	
	
	
	public Client() {
		readIni();
		connectToServer();
	}


	private void readIni() {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));
			clusterMembers = p.getProperty("cluster");
			address = new Address(p.getProperty("ip"), Integer.parseInt(p.getProperty("port")));

		} catch (IOException e) {
			e.printStackTrace();
		}
		clusterMembersVector = clusterMembers.split(";");
	}


	public String generateFullLog(String log) {
		String logID = "clientID" + System.currentTimeMillis();
		String generatedLog = logID + "-" + log;
		return generatedLog;
	}


	public void connectToServer() {
		if(tryCount==clusterMembersVector.length-1)
			return;
		
		//Suggestion use a for loop instead of a recursive method
		tryCount++;
		leaderIp = clusterMembersVector[tryCount].split(":")[0];
		leaderPort = clusterMembersVector[tryCount].split(":")[1];
		System.out.println("Request ->"+leaderIp + ":"+leaderPort);

		try {

			look_up = (LeaderBehaviour) Naming.lookup("rmi://" + leaderIp + ":" + leaderPort + "/server");

			ServerResponse response = look_up.request(generateFullLog("abcdtest"));
			// If the Object of the ServerResponse instance is null, that means it received
			// the Address of the leader. Try reconnect to leader
			System.out.println(response.toString());
			if (response.getResponse()==null) {
				leaderIp = response.getLeader().getIpAddress();
				leaderPort = String.valueOf(response.getLeader().getPort());
				look_up = (Server) Naming.lookup("rmi://" + leaderIp + ":" + leaderPort + "/server");

				response = look_up.request(generateFullLog("abcdtest"));
			}

			System.out.println(response.toString());

		} catch (NotBoundException | MalformedURLException | RemoteException e) {
			System.out.println("This cluster member is offline");
			connectToServer();
		}
	}


	public ServerResponse executeCommand(String command) throws RemoteException {
		//TODO façam aqui a vossa lógica dos logs
		return look_up.request(command);
	}
	
}
