package com.raft.models;

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
}
