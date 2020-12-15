package com.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;

import com.raft.LeaderBehaviour;
import com.raft.models.Address;
import com.raft.models.ServerResponse;

import javafx.beans.property.SimpleObjectProperty;
import lombok.Data;

@Data
public class Client {

	private Address[] clusterArray;
	private LeaderBehaviour leader;
	private String clientID;
	
	private SimpleObjectProperty<Address> leaderAddress = new SimpleObjectProperty<>();
	
	public Client() {
		clientID = Address.getLocalIp();
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
			p.load(new FileInputStream("src/main/resources/client/config.ini"));
			String[] clusterString = p.getProperty("cluster").split(";");
			clusterArray = new Address[clusterString.length];
			for (int i = 0; i < clusterString.length; i++) {
				String[] splited = clusterString[i].split(":");
				clusterArray[i] = new Address(splited[0], Integer.parseInt(splited[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		for (int i = 0; i < clusterArray.length; i++) {
			Address address = clusterArray[i];
			leaderAddress.set(address);
			try { 

				leader = (LeaderBehaviour) Naming.lookup("rmi://" + address.getIpAddress() + ":" + address.getPort() + "/leader");
				ServerResponse response = leader.execute("", generateCommandID(clientID));
				
//				 If the Object of the ServerResponse instance is null, that means it received
//				 the Address of the leader. Try reconnect to leader
				if (response.getResponse() == null) {
					leader = (LeaderBehaviour) Naming.lookup("rmi://" + response.getLeader().getIpAddress() + ":" + response.getLeader().getPort() + "/leader");
					response = leader.execute("", generateCommandID(clientID));
					if (response.getResponse() == null) 
						return;
				}
			} catch (Exception e) {
				System.err.println("Could not connect to: "+address);
				continue;
			}
		}
		System.out.println(leader == null ? "No leader Found" : "Connected to "+leaderAddress.get());
	}


	
	/**
	 * Request a message to server
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
				to_return = leader.execute(command, operationID);
				break;
			} catch (RemoteException | NullPointerException e) {
				connectToServer();
			}
		}
		if(to_return == null)
			ClientController.getInstance().showErrorDialog("Failed to connect","Could not connect to leader. Please check your internet connection");
		return to_return;
	}
	
	
	
	public void startInfiniteRequests() {
		String op = "put:Var:Value";
		String commandID = generateCommandID(clientID);
		boolean retry = true;
		while(true) { 
//			try {
//				Thread.sleep(10);
//			} catch (InterruptedException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
			if(!retry)
				commandID = generateCommandID(clientID);
			try {
				ServerResponse resp = leader.execute(op, commandID);
				if(resp.getResponse() == null)
					connectToServer();
				else
					retry = false;
			} catch (Exception e) {
				System.out.println("Couldn't execute action");
				retry = true;
				connectToServer();
			}
		}
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
