package com.raft.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class that represents the the IP and port of a Server.
 * This class is also the server id
 * @author RuiMenoita
 *
 */
@Data
@AllArgsConstructor
public class Address implements Serializable{
	private String ipAddress; //IP address
	private int port;		  //port

	
	/**
	 * Parses the given string into Address object
	 * String format : //TODO
	 * @param s string to be parsed
	 * @return returns the Address parsed or null if it could not parse the given string
	 */
	public static Address parse(String s) {
		if(s == null || s.isBlank())
			return null;
		//TODO
		return null;
	}

	
	
	
	
	/**
	 * @return returns local network IP
	 */
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


	
	
	/**
	 * @return returns public network IP
	 */
	public static String getPublicIp() {
		try {
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			return in.readLine();
		} catch (IOException e) {e.printStackTrace();}
		return null;
	}
}
