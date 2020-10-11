package com.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.Properties;

import com.raft.Server;

public class ServerLauncher {
	public static void main(String[] args) throws Exception{
		Properties p = new Properties();
		p.load(new FileInputStream("src/main/resources/config.ini"));
		System.out.println("Starting servers...");
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(2000);
		System.out.println("Server 1 up");
		p.setProperty("port", ""+1001);
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(2000);
		System.out.println("Server 2 up");
		p.setProperty("port", ""+1002);
		new Thread (() -> {try {new Server(p);} catch (IOException | AlreadyBoundException e) {}}).start();
		Thread.sleep(2000);
		System.out.println("Server 3 up");
	}
}
