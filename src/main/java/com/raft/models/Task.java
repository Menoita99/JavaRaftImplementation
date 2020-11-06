package com.raft.models;

public class Task implements Runnable{
	private Runnable r;
	public Task(Runnable r) {
		this.r = r;
	}
	@Override
	public void run() {
		r.run();
	}
}
