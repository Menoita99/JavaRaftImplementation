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
import java.util.Random;

import com.raft.LeaderBehaviour;
import com.raft.models.Address;
import com.raft.models.ServerResponse;

import javafx.beans.property.SimpleStringProperty;
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

	private SimpleStringProperty leaderPort;
	private SimpleStringProperty leaderIp;



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
			Random random = new Random();
			address = new Address(p.getProperty("ip"), random.nextInt(10000) + 1010);

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
		leaderIp = new SimpleStringProperty(clusterMembersVector[tryCount].split(":")[0]);
		leaderPort = new SimpleStringProperty(clusterMembersVector[tryCount].split(":")[1]);
		System.out.println("Request ->"+leaderIp.get() + ":"+leaderPort.get());

		try {

			look_up = (LeaderBehaviour) Naming.lookup("rmi://" + leaderIp.get() + ":" + leaderPort.get() + "/leader");

//			ServerResponse response = look_up.execute(generateFullLog("try_connection"));
//			// If the Object of the ServerResponse instance is null, that means it received
//			// the Address of the leader. Try reconnect to leader
//			if (response.getResponse()==null) {
//				leaderIp.set(response.getLeader().getIpAddress());
//				leaderPort.set(String.valueOf(response.getLeader().getPort()));
//				look_up = (LeaderBehaviour) Naming.lookup("rmi://" + leaderIp.get() + ":" + leaderPort.get() + "/leader");
//				response = look_up.execute(logsList.get(0));
//				System.out.println("Follower answer:"+response);
//
//			}
//			logsList.clear();
		} catch (NotBoundException | MalformedURLException | RemoteException e) {
			e.printStackTrace();
			System.out.println("This cluster member is offline");
			connectToServer();
		}
	}


	public ServerResponse request(String command) throws RemoteException {
		ServerResponse to_return = null;
		//3 tries
		for (int i = 0; i < 3; i++) {
			try {
				to_return = look_up.execute(command);
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

}
