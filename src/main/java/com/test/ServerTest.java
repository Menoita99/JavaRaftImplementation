package com.test;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;

import com.raft.Server;

public class ServerTest {
	public static void main(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException{
		new Server();
	}
}
