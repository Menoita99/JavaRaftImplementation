package com.raft.models;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppendResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private long term;
	private boolean success;
}
