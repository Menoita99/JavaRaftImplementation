package com.raft.util;

import java.util.ArrayDeque;
import java.util.Queue;

public class BlockingQueue<T> {

	private Queue<T> queue= new ArrayDeque<T>();
	private final int capacity;

	public BlockingQueue() {
		capacity=-1;	
	}

	public BlockingQueue(int c) {
		if(c<=0)	throw new IllegalArgumentException("Invalid Size");
		capacity=c;			
	}

	public synchronized void Offer(T e) throws InterruptedException {
		assert(e !=null);
		while(queue.size()>capacity && capacity!=-1) 
			wait();

		queue.add(e);
		notifyAll();
	}


	public synchronized T poll() throws InterruptedException {
		while(queue.isEmpty()) 
			wait();

		T e = queue.remove();
		notifyAll();
		return e;
	}

	public int size() {
		return queue.size();
	}

	public void clear() {
		queue.clear();
	}	

	@Override
	public String toString() {
		return "BlockingQueue [queue=" + queue + ", capacity=" + capacity + "]";
	}
}
