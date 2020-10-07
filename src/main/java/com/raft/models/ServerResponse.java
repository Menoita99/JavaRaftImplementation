package com.raft.models;

import java.io.Serializable;

import com.raft.Server;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * Class that represents the response for invoking the request method in {@link Server}
 * @author RuiMenoita
 */
@Data
@AllArgsConstructor
public class ServerResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private Address leader;    //leaders address
	private Object response;   //response to the command requested 
}
