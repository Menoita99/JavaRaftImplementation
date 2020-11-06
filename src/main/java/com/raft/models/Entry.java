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
public class Entry implements Serializable {
	private static final long serialVersionUID = 1L;
	private long index;		  //log index
	private long term;		  //log term
	private String command;   //command request by client
	private String commandID;
	
	public String toFileString(Entry l) {
		return l.getIndex()+"|"+l.getTerm()+"|"+l.getCommandID()+"|"+l.getCommand().replace("\n", "\\n");
	}

	public String toFileString() {
		return this.getIndex()+"|"+this.getTerm()+"|"+this.commandID+"|"+this.getCommand().replace("\n", "\\n");
	}
	
	
	public static Entry fromString(String s) {
		String[] splited = s.split("\\|");
		return new Entry(Long.parseLong(splited[0]), Long.parseLong(splited[1]), splited[2], splited[3]);
	}
	
}
