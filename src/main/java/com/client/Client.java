package com.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

import com.raft.LeaderBehaviour;
import com.raft.models.Address;
import com.raft.models.ServerResponse;

import javafx.beans.property.SimpleStringProperty;
import lombok.Data;

@Data
public class Client {

	
	private Address address;

	private String clusterMembers ;
	private int tryCount = -1;
	private String clusterMembersVector[];
	private LeaderBehaviour look_up;

	private SimpleStringProperty leaderPort;
	private SimpleStringProperty leaderIp;
	
	

	private String clientID;
	
	
	
	public Client() {
		leaderIp = new SimpleStringProperty();
		leaderPort = new SimpleStringProperty();
		try {
			clientID = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		readIni();
		connectToServer();
		
	}



	/**
	 * Reads config.ini located at "src/main/resources" and fills a string with all cluster members' addresses
	 * Generates a random port number.
	 */
	private void readIni() {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/config.ini"));
			clusterMembers = p.getProperty("cluster");
			address = new Address(clientID, 0);

		} catch (IOException e) {
			e.printStackTrace();
		}
		clusterMembersVector = clusterMembers.split(";");
	}


	
	/**
	 * Receives log and adds a unique ID to said log
	 * @param log
	 * @return received log, along with a unique ID
	 */
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
		System.out.println(generatedLog);
		return generatedLog;
	}


	
	
	/**
	 * Connects to server
	 * Gets the ip and port of the first cluster member listed and attempts connection, 
	 * if this member is offline tries again with the next member, and so on.
	 * 
	 */
	public void connectToServer() {
		if(tryCount==clusterMembersVector.length-1) {
			return;
		}

		tryCount++;
		SimpleStringProperty ipToConnect = new SimpleStringProperty(clusterMembersVector[tryCount].split(":")[0]);
		SimpleStringProperty portToConnect = new SimpleStringProperty(clusterMembersVector[tryCount].split(":")[1]);
		System.out.println("Request ->"+ipToConnect.get() + ":"+portToConnect.get());

		try {

			look_up = (LeaderBehaviour) Naming.lookup("rmi://" + ipToConnect.get() + ":" + portToConnect.get() + "/leader");
			ServerResponse response = look_up.execute("", generateCommandID(clientID));
			
//			 If the Object of the ServerResponse instance is null, that means it received
//			 the Address of the leader. Try reconnect to leader
			if (response.getResponse()==null) {
				System.out.println("Received message from follower, attempting leader connect");
				leaderIp.set(response.getLeader().getIpAddress());
				leaderPort.set(String.valueOf(response.getLeader().getPort()));
				
				look_up = (LeaderBehaviour) Naming.lookup("rmi://" + leaderIp.get() + ":" + leaderPort.get() + "/leader");
				response = look_up.execute("", generateCommandID(clientID));
			}
			
		} catch (NotBoundException | MalformedURLException | RemoteException e) {
			e.printStackTrace();
			System.out.println("This cluster member is offline");
			connectToServer();
		}
	}


	
	/**
	 * 
	 * @param command
	 * @return
	 * @throws RemoteException
	 */
	public ServerResponse request(String command) throws RemoteException {
		ServerResponse to_return = null;
		String operationID = generateCommandID(clientID);
		//3 tries
		for (int i = 0; i < 3; i++) {
			try {
				to_return = look_up.execute(command, operationID);
				break;
			} catch (RemoteException | NullPointerException e) {
				e.printStackTrace();
				connectToServer();
			}
		}
		if(to_return == null) {
			ClientController.getInstance().showErrorDialog("Failed to connect","Could not connect to leader. Please check your internet connection");
			throw new  IllegalStateException("Unable to connect");
		}
		return to_return;
	}

	
	/**
	 * Generates unique ID
	 * @param clientID
	 * @return clientID + timestamp
	 */
	public String generateCommandID (String clientID) {
		return clientID + ":" +System.currentTimeMillis();
	}
}
