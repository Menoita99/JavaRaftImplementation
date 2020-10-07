package com.raft.models;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class that represents a vote response from a server
 * @author RuiMenoita
 */
@Data
@AllArgsConstructor
public class VoteResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private long term;               //currentTerm, for candidate to update itself
	private boolean voteGranted;     //true means candidate received vote
}
