package com.raft.models;

import java.io.Serializable;

import com.raft.Server;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class that represents the response for invoking the appendEntries method in {@link Server}
 * @author RuiMenoita
 */
@Data
@AllArgsConstructor 
public class AppendResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private Entry lastEntry;//current index
	private long term;			//current server term
	private boolean success;	// has appended or not entries
}