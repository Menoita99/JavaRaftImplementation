package com.client;

import java.awt.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

public class Client {
/*
 * 1-cliente liga e conecta-se a um server random
 * 			se nao for lider o follower rencaminha po lider e este faz o pedido de novo
 * 			se o lider crasha ele volta a escolher um server random
 * 
 * cada comando do cliente tem o mesmo id
 * 
 * 
 * 
 * 
 * */
	private String port,clusterString,timeOutIntervalString;

	private ObjectOutputStream outToServer;
	private ObjectInputStream inFromServer;
	
	private ArrayList<String[]> clusterList;
	
	public Client() {
		readIni();
		connectToServer();
	}
	
	private void readIni() {
		port="";
		clusterList = new ArrayList<>();
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));
			
			port = p.getProperty("port");
			clusterString = p.getProperty("cluster");
			timeOutIntervalString = p.getProperty(timeOutIntervalString);
		
		} catch (IOException e) {
			System.err.println("Port : -> " + port + "\nClusterString : -> " + clusterString + "\n timeOutIntervalString : -> " +timeOutIntervalString);
			e.printStackTrace();
		}
		String clusterVector[] = clusterString.split(";");
		for(String ipPort : clusterVector) {
			clusterList.add(ipPort.split(":"));
		}

	}

	public void connectToServer() {
		Socket clientSocket;
		try {
			InetAddress host = InetAddress.getLocalHost();
			clientSocket = new Socket(host, Integer.parseInt(port));

			outToServer = new ObjectOutputStream(clientSocket.getOutputStream());
			inFromServer = new ObjectInputStream(clientSocket.getInputStream());

			outToServer.writeObject("client");

			while (true) {
				try {
					Object o = inFromServer.readObject();
					
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
}
