package com.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;

import com.raft.Server;
import com.raft.models.Task;
import com.raft.util.ThreadPool;

import lombok.Data;

@Data
public class MonitorClient {

	private ThreadPool monitorThread = new ThreadPool(1);

	private MonitorBehaviour monitorServer;
	private Server server;


	public MonitorClient(Server server){ 
		this.server = server;
		monitorThread.submit(new Task(()->{
			try {
				Properties p = new Properties();
				p.load(new FileInputStream(server.getRoot()+File.separator+Server.CONFIG_INI));
				monitorServer = (MonitorBehaviour) Naming.lookup("rmi://" + p.getProperty("monitorIp") + ":" + p.getProperty("monitorPort") + "/monitor");
			}catch (Exception e) {
				e.printStackTrace();
			}
		}));
	} 



	public void startedElection() {
		monitorThread.submit(new Task(()->{
			try {
				monitorServer.startedElection(new MonitorRequest(server));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}));
	}

	
	public void newLeader() {
		monitorThread.submit(new Task(()->{
			try {
				monitorServer.newLeader(new MonitorRequest(server));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}));
	}

	public void commandEval() {
		monitorThread.submit(new Task(()->{
			try {
				monitorServer.commandEval();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}));
	}

	public void updateStatus() {
		monitorThread.submit(new Task(()->{
			try {
				monitorServer.updateStatus(new MonitorRequest(server));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}));
	}
}
