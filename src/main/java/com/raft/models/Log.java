package com.raft.models;

import java.io.Serializable;

import lombok.Data;

@Data
public class Log implements Serializable {
	private static final long serialVersionUID = 1L;
	private String command;
	private long term;
	private long index;
}
