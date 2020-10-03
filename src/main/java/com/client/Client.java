package com.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;

public class Client {
	
/*
 * 1-cliente liga e conecta-se a um server random
 * 			se nao for lider o follower rencaminha po lider e este faz o pedido de novo
 * 			se o lider crasha ele volta a escolher um server random
 * 
 * cada comando do cliente tem o mesmo id
 **/
	
	private String port,clusterString,timeOutIntervalString,ipServer;
	private String clientID;
	private ObjectOutputStream outToServer;
	private ObjectInputStream inFromServer;
	
	private ArrayList<String[]> clusterList;
	
	public Client() {
		readIni();
		connectToServer();
	}
	
	private void readIni() {
		port="";
		ipServer="";
		clusterList = new ArrayList<>();
		
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));
			
			port = p.getProperty("port");
			ipServer = p.getProperty("ip");
			clusterString = p.getProperty("cluster");
			timeOutIntervalString = p.getProperty("timeOutInterval");
			clientID = ipServer + port;
			clusterString += "";
		
		} catch (IOException e) {
			System.err.println("Port : -> " + port + "\nClusterString : -> " + clusterString + "\n timeOutIntervalString : -> " +timeOutIntervalString);
			e.printStackTrace();
		}
		String clusterVector[] = clusterString.split(";");
		for(String ipPort : clusterVector) {
			clusterList.add(ipPort.split(":"));
			System.out.println(ipPort);
		}
	}

	public String generateFullLog(String log) {
		String logID = clientID + System.currentTimeMillis();
		String generatedLog = logID + "-" + log;	
		return generatedLog;
	}
	
	public void connectToServer() {
		Socket clientSocket;
		try {
			clientSocket = new Socket(ipServer, getClusterPorts());
			
			inFromServer = new ObjectInputStream(clientSocket.getInputStream());
			outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
			while (true) {				
				String message = "";
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	private int getClusterPorts() {
		//return clusterList.get(Math.floor(Math.random() * clusterList.size()));
	return 0;
	}
	
}
