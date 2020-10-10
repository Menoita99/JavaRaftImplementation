package com.raft.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Operation {
	
	private String operationID;
	private Object response;
	
	
}
