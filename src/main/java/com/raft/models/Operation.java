package com.raft.models;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Operation implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String operationID;
	private Object response;
}
