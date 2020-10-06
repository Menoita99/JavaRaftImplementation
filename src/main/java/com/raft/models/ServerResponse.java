package com.raft.models;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServerResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private Address leader;
	private Object response;
	
	
	public ServerResponse (Address leader, Object response) {
		this.leader = leader;
		this.response = response;
	}


	public Address getLeader() {
		return leader;
	}


	public Object getResponse() {
		return response;
	}
	
	
}
