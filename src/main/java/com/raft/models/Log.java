package com.raft.models;

import java.io.Serializable;

import lombok.Data;

/**
 * Class that represents a log
 * @author RuiMenoita
 */
@Data
public class Log implements Serializable {
	private static final long serialVersionUID = 1L;
	private String command;   //command request by client
	private long term;		  //log term
	private long index;		  //log index
	
	public String toFileString(Log l) {
		return l.getIndex()+";"+l.getTerm()+";"+l.getCommand().replace("\n", "\\n");
	}

	public String toFileString() {
		return this.getIndex()+";"+this.getTerm()+";"+this.getCommand().replace("\n", "\\n");
	}
}
