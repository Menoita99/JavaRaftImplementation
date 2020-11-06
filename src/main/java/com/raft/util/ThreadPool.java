package com.raft.util;

public class ThreadPool {
	
	private BlockingQueue<Runnable> tasks = new BlockingQueue<>();
	private Worker[] workers;
	
	public ThreadPool(int n) {
		this.workers= new Worker[n];
		for(int i =0; i<workers.length;i++) {
			workers[i]= new Worker();
			workers[i].start();
		}
	}
	
	public void submit(Runnable task){
		assert(task != null);
		try {
			tasks.Offer(task);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private class Worker extends Thread{
		
		 @Override
		public void run() {
			while(true) {
				try {
					tasks.poll().run();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
