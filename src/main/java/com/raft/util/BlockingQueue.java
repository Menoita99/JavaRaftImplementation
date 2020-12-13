package com.raft.util;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Queue;

public class BlockingQueue<T>  implements Serializable{
	
	private static final long serialVersionUID = 1L;

	private Queue<T> queue= new ArrayDeque<T>();
	private final int capacity;

	private int discardLimit = -1;

	public BlockingQueue() {
		capacity=-1;	
	}

	public BlockingQueue(int c) {
		if(c<=0)	throw new IllegalArgumentException("Invalid Size");
		capacity=c;			
	}
	
	public void setDiscardLimit(int n) {
		this.discardLimit  = n;
	}

	public synchronized void Offer(T e) throws InterruptedException {
		if(discardLimit>0 && queue.size() > discardLimit)
			return;
		
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
