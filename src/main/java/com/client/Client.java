package com.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

import com.raft.LeaderBehavior;
import com.raft.Server;
import com.raft.models.Address;
import com.raft.models.ServerResponse;
import com.rmi.RMIInterface;

public class Client {

	/*
	 * 1-cliente liga e conecta-se a um server random se nao for lider o follower
	 * rencaminha po lider e este faz o pedido de novo se o lider crasha ele volta a
	 * escolher um server random
	 * 
	 * cada comando do cliente tem o mesmo id
	 **/

	private String port, clusterMembers, timeOutIntervalString, ipServer;
	private String clientID;
	private int tryCount = -1;
	private String clusterMembersVector[];
	private static RMIInterface look_up;

	public Client() {
		readIni();
		connectToServer();
	}

	private void readIni() {

		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));

			clusterMembers = p.getProperty("cluster");
			timeOutIntervalString = p.getProperty("timeOutInterval");
			//			clientID = ipServer + port;

		} catch (IOException e) {
			//			System.err.println("Port : -> " + port + "\nClusterString : -> " + clusterMembers + "\n timeOutIntervalString : -> " +timeOutIntervalString);
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
		if(tryCount==clusterMembersVector.length)
			return;
		tryCount++;
		String ip = clusterMembersVector[tryCount].split(":")[0];
		String port = clusterMembersVector[tryCount].split(":")[1];
		System.out.println(ip + ":"+port);
		Address address = new Address(ip, Integer.parseInt(port));
		
		try {
			
			look_up = (RMIInterface) Naming.lookup("rmi://" + ip + ":" + port + "/server");
			ServerResponse response = look_up.request(generateFullLog("abcdtest"));

			// If the Object of the ServerResponse instance is null, that means it received
			// the Address of the leader. Try reconnect to leader
			if (response.getResponse()==null) {
				ip = response.getLeader().getIpAddress();
				port = String.valueOf(response.getLeader().getPort());
				look_up = (Server) Naming.lookup("rmi://" + ip + ":" + port + "/server");

				response = look_up.request(generateFullLog("abcdtest"));
			}

			System.out.println(response.toString());
			
		} catch (NotBoundException | MalformedURLException | RemoteException e) {
			System.out.println("This cluster member is offline");
			connectToServer();
		}
	}

	//            Java rmi code for client  
	//			  Address add =  new Address(ip, port);
	//            Server server = (Server) Naming.lookup("rmi://"+ip+":"+"port"+port+"/server");
	//            ServerResponse response = server.request(params...);
	//            System.out.println("response: " + response);

	public static void main(String[] args) {
		new Client();
	}
	

}
