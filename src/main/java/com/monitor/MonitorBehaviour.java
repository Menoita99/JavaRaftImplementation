package com.monitor;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface MonitorBehaviour extends Remote {
	
	
	void startedElection(MonitorRequest request) throws RemoteException;
	
	void newLeader(MonitorRequest request) throws RemoteException;
	
	void commandEval() throws RemoteException;
	
	void updateStatus(MonitorRequest request) throws RemoteException;
}
