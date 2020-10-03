package com.raft.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Address {
	private String ipAddress;
	private int port;



	public static Address parse(String s) {
		if(s == null || s.isBlank())
			return null;
		//TODO
		return null;
	}

	

	public static String getLocalIp() {
		Socket socket = null;
		try{
			socket = new Socket();
			try {
				socket.connect(new InetSocketAddress("google.com", 80));
			} catch (IOException e) {}
			return socket.getLocalAddress().getHostAddress();
		}finally {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	
	
	public static String getPublicIp() {
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			return in.readLine();
		} catch (IOException e) {e.printStackTrace();}
		return null;
	}
}
