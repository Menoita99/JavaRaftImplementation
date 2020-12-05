package com.monitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import com.raft.models.Address;

public class MonitorServer implements MonitorBehaviour{

	private static MonitorServer instance;
	private Address[] clusterArray;
	private Address selfId;
	private int evals = 0;
	private Timer timer = new Timer();
	private MonitorController controller = MonitorController.getInstance(); 

	public MonitorServer(){
		readIni();
		registServer();
		timer.schedule(new TimerTask() {
			@Override 
			public void run() {
				controller.updateChart(evals);
				evals = 0;
			}
		}, 0, 1000);
	}

	private void registServer(){
		try {
			Registry registry = LocateRegistry.createRegistry(selfId.getPort());
			Object server = UnicastRemoteObject.exportObject(this, 0);
			System.setProperty( "java.rmi.server.hostname", "127.0.0.1");
			registry.bind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/monitor", (MonitorBehaviour)server);
			Naming.rebind("rmi://"+selfId.getIpAddress()+":"+selfId.getPort()+"/monitor", (MonitorBehaviour)server);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}


	private void readIni() {
		try {
			Properties p = new Properties();
			p.load(new FileInputStream("src/main/resources/Monitor/config.ini"));

			selfId = new Address(p.getProperty("monitorIp"), Integer.parseInt(p.getProperty("monitorPort")));

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



	@Override
	public void startedElection(MonitorRequest request) {
		controller.updateTableBottomStatus(request,clusterArray);
		controller.addHistoricEntry(request);
	}




	@Override
	public void downGradetoFollow(MonitorRequest request) throws RemoteException {
		controller.updateTableBottomStatus(request,clusterArray);
		controller.addHistoricEntry(request);
	}
	
	


	@Override
	public void newLeader(MonitorRequest request) {
		controller.setLeaderLabelText("Leader: "+request.getSender());
		controller.updateTableBottomStatus(request,clusterArray);
		controller.addHistoricEntry(request);
	}




	@Override
	public void commandEval() {
		evals++;
	}





	@Override
	public void updateStatus(MonitorRequest request) {
		controller.updateTableBottomStatus(request,clusterArray);
	}

	
	
	public static MonitorServer getInstance() {
		if(instance == null)
			instance = new MonitorServer();
		return instance;
	}

}
