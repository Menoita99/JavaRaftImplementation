package com.test;

import java.io.IOException;
import java.rmi.AlreadyBoundException;

import com.raft.Server;

public class ServerTest {
	public static void main(String[] args) throws AlreadyBoundException, IOException{
		new Server();
	}
}
