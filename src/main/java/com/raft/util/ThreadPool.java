package com.raft.util;

import java.io.Serializable;

public class ThreadPool  implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
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
	
	/**
	 * @return returns true if at least one worker is waiting for a task
	 */
	public boolean isWorkerAvailable() {
		for (Worker worker : workers)
			if(worker.getState() == Thread.State.BLOCKED || worker.getState() == Thread.State.WAITING)
				return true;
		return false;
	}
	
	private class Worker extends Thread implements Serializable{
		
		private static final long serialVersionUID = 1L;
		
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
