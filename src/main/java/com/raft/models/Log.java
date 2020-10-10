package com.raft.models;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Class that represents a log
 * @author RuiMenoita
 */
@Data
@AllArgsConstructor
public class Log implements Serializable {
	private static final long serialVersionUID = 1L;
	private long index;		  //log index
	private long term;		  //log term
	private String command;   //command request by client
	
	public String toFileString(Log l) {
		return l.getIndex()+"|"+l.getTerm()+"|"+l.getCommand().replace("\n", "\\n");
	}

	public String toFileString() {
		return this.getIndex()+"|"+this.getTerm()+"|"+this.getCommand().replace("\n", "\\n");
	}
}
