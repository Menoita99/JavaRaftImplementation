package com.raft.state;

import java.io.Serializable;
import java.util.HashMap;

import com.raft.Server;
import com.raft.models.Address;

import lombok.Data;

/**
 * This class represents the Leader State that store
 * other servers data to send requests
 * @author RuiMenoita
 */
@Data
public class LeaderState implements Serializable{
	private static final long serialVersionUID = 1L;
	private HashMap<Address, Long> nextIndex = new HashMap<>();
	private HashMap<Address, Long> matchIndex = new HashMap<>();


	/**
	 * Resents current state
	 */
	public void reset(Server s) {
		nextIndex.clear();
		matchIndex.clear();
		Address[] servers = s.getClusterArray();
		for (int i = 0; i < servers.length; i++) {
			nextIndex.put(servers[i], s.getState().getLastLog().getIndex()+1);
			nextIndex.put(servers[i], 0L);
		}
	}
}
