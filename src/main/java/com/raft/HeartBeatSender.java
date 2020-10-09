package com.raft;

import com.raft.state.Mode;

public class HeartBeatSender extends Thread {
	
	private Server server;
	private boolean isRunning;

	public HeartBeatSender(Server server) {
		this.server = server;
	}
	
	
	@Override
	public void run() {
		while(isRunning) {
			if(isLeader()) {
				server.sendAppendEntriesRequest(null);
				try {
					sleep(Server.HEARTBEAT_TIME_OUT);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private synchronized boolean isLeader() {
		while(server.getMode() != Mode.LEADER) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	
	
	
	public synchronized void notifyLeader() {
		notifyAll();
	}
	
	
	


	@Override
	public synchronized void start() {
		isRunning = true;
		super.start();
	}
}
