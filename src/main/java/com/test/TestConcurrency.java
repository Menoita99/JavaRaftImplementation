package com.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Properties;

import com.raft.LeaderBehaviour;
import com.raft.Server;

public class TestConcurrency {
	public static void main(String[] args) throws Exception{
		Properties p = new Properties();
		p.load(new FileInputStream("src/main/resources/config.ini"));
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(2000);
		p.setProperty("port", ""+1001);
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(2000);
		p.setProperty("port", ""+1002);
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(2000);
		TestClientConcurrencie();
	}

	private static void TestClientConcurrencie() throws NotBoundException, MalformedURLException, RemoteException {
		LeaderBehaviour l = (LeaderBehaviour) Naming.lookup("rmi://127.0.0.1:1000/leader");
		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				try {
					l.execute("1+1", "127.0.0.1:"+i);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				try {
					l.execute("sleep(15)", "127.0.0.2:"+i);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				try {
					l.execute("2+2", "127.0.0.3:"+i);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				try {
					l.execute("sleep(1000)", "127.0.0.4:"+i);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
