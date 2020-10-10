package com.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Properties;

import com.raft.LeaderBehaviour;
import com.raft.Server;

public class ServerTest {
	public static void main(String[] args) throws Exception{
		Properties p = new Properties();
		p.load(new FileInputStream("src/main/resources/config.ini"));
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(1000);
		p.setProperty("port", ""+1001);
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(1000);
		p.setProperty("port", ""+1002);
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(1000);
		LeaderBehaviour l = (LeaderBehaviour) Naming.lookup("rmi://127.0.0.1:1000/leader");
		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				try {
					l.execute("1+1", i+"");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}).start();
		new Thread(() ->{
			for (int i = 0; i < 10; i++) {
				try {
					l.execute("2+2", i+"");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
