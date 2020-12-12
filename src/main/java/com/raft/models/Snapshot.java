package com.raft.models;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.raft.state.ServerState;

import lombok.Data;

@Data
public class Snapshot implements Serializable{

	private static final long serialVersionUID = 1L;

	public static final String SNAP_FILE_NAME = "snapshot.dat";
	
	
	private ServerState state;


	public Snapshot(ServerState serverState) {
		this.state = serverState;
	}
	
	public void snap() {
		state.getLock().lock();
		try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(state.getRootPath()+File.separator+SNAP_FILE_NAME)))){
			out.writeObject(state);
			System.out.println("[Snapshot] Made snapshot "+LocalDateTime.now());
			state.clearLogFile();
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			state.getLock().unlock();
		}
	}
	
	
	
	public static ServerState recoverfromFile(String path) throws Exception {
		try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(path+File.separator+SNAP_FILE_NAME)))){
			ServerState state = (ServerState) in.readObject();
			state.init();
			state.getInterpreter().getPool().resuscitateDeadWorkers();
			return state;
		}
	}
}
