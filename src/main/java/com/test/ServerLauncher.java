package com.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.Properties;

import com.raft.Server;

public class ServerLauncher {
	public static void main(String[] args) throws Exception{
		System.out.println("Starting servers...");
		for (int i = 0; i < 3; i++) {
			try {
				Properties p = new Properties();
				p.load(new FileInputStream("src/main/resources/config.ini"));
				p.setProperty("port", "100"+i);
				Server s = new Server(p);
				System.out.println("Server "+i+" up "+s.getSelfId());
			} catch (IOException | AlreadyBoundException e) {}
		}
	}
}
