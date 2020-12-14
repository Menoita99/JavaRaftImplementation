package com.raft.models;

import java.io.Serializable;

public class Task implements Runnable,Serializable{
	private static final long serialVersionUID = 1L;
	private Runnable r;
	public Task(Runnable r) {
		this.r = r;
	}
	@Override
	public void run() {
		r.run();
	}
}
