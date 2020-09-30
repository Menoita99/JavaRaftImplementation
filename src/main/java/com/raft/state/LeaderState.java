package com.raft.state;

import java.io.Serializable;
import java.util.HashMap;

import com.raft.models.Address;

import lombok.Data;

@Data
public class LeaderState implements Serializable{
	private static final long serialVersionUID = 1L;
	private HashMap<Address, Long> nextIndex = new HashMap<>();
	private HashMap<Address, Long> matchIndex = new HashMap<>();


	public void reset() {
		nextIndex.clear();
		matchIndex.clear();
	}
}
