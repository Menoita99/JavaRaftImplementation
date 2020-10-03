package com.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.util.Properties;

import com.raft.Server;
import com.raft.models.Address;

public class Client {
	
/*
 * 1-cliente liga e conecta-se a um server random
 * 			se nao for lider o follower rencaminha po lider e este faz o pedido de novo
 * 			se o lider crasha ele volta a escolher um server random
 * 
 * cada comando do cliente tem o mesmo id
 **/
	
	private String port,clusterMembers,timeOutIntervalString,ipServer;
	private String clientID;
	private ObjectOutputStream outToServer;
	private ObjectInputStream inFromServer;
	private int tryCount = -1;
	private String clusterMembersVector[];
	
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
		
		
		
//		for(String ipPort : clusterMembersVector) {
//			clusterList.add(ipPort.split(":"));
//			System.out.println(ipPort);
//		}
	}

	public String generateFullLog(String log) {
		String logID = clientID + System.currentTimeMillis();
		String generatedLog = logID + "-" + log;	
		return generatedLog;
	}
	
	public void connectToServer() {
		Socket clientSocket;
		tryCount++;
		try {
			String ip = clusterMembersVector[tryCount].split(":")[0];
			String port = clusterMembersVector[tryCount].split(":")[1];
			
			clientSocket = new Socket(ip, Integer.parseInt(port));
			
			inFromServer = new ObjectInputStream(clientSocket.getInputStream());
			outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
			
//            Java rmi code for client  
//			  Address add =  new Address(ip, port);
//            Server server = (Server) Naming.lookup("rmi://"+ip+":"+"port"+port+"/server");
//            ServerResponse response = server.request(params...);
//            System.out.println("response: " + response);
			
			
		} catch (UnknownHostException | ConnectException e) {
			System.out.println("This cluster member is offline");
			connectToServer();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private int getClusterPorts() {
		//return clusterList.get(Math.floor(Math.random() * clusterList.size()));
	return 0;
	}
	
}
