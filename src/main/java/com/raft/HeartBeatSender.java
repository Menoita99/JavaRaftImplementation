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
			try {
				waitUntilServerIsLeader();
				server.getEntityManager().submitEntry(null);
				sleep(server.getHeartbeatTimeOut());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}





	/**
	 * check if server is leader if is not waits until it is
	 * @throws InterruptedException 
	 */
	private synchronized void waitUntilServerIsLeader() throws InterruptedException {
		while(server.getMode() != Mode.LEADER) 
			wait();
	}




	/**
	 * Wakes heartbeater thread to check isLeader() condition
	 */
	public synchronized void goOn() {
		notifyAll();
	}





	@Override
	public synchronized void start() {
		isRunning = true;
		super.start();
	}
}
