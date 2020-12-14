package com.raft.util;


import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class OneSchedualTimer{

	private Worker worker ;

	@Getter
	@Setter
	private Runnable task;

	private long startExecution;

	public OneSchedualTimer() {
		worker = new Worker();
		worker.start();
	}

	public OneSchedualTimer(Runnable task) {
		this.task = task;
		worker = new Worker();
		worker.start();
	}

	public OneSchedualTimer(Runnable task,long millis){
		this.task = task;
		worker.start();
		worker = new Worker(millis);
		schedual(millis);
	}

	public void schedual(long millis){
		worker.submit(millis);
	}

	public void schedual(Runnable task,long millis){
		this.task = task;
		if(worker != null && !worker.isBusy())
			worker.submit(millis);
	}

	public void restart(long millis){
		stop();
		worker = new Worker(millis);
		worker.start();
	}

	public void stop() {
		if(worker != null)
			worker.setCanceled(true);
		if(worker != null && !worker.isWorking())
			worker.interrupt();
		worker = null;
	}

	public boolean isBusy(){
		return worker == null || worker.isBusy();
	}

	public long timeLeftUntilExecution(){
		return Math.max(0, startExecution == 0 ? worker.millis : System.currentTimeMillis()-startExecution);
	}



	@Getter
	@Setter
	@NoArgsConstructor
	private class Worker extends Thread implements Serializable{
		private static final long serialVersionUID = 1L;

		private boolean canceled = false;
		private boolean busy = false;
		private boolean working = false;
		private long millis;




		public Worker(long millis) {
			this.millis = millis;
		}

		public void submit( long millis) {
			this.millis = millis;
			synchronized (this) {
				notify();
			}
		}


		@Override
		public void run() {
			try {
				while(task == null) {
					busy = false;
					synchronized (this) {
						wait();
					}
				}
				busy = true;
				startExecution = System.currentTimeMillis();
				Thread.sleep(millis);

				if(!canceled) {
					working = true;
					task.run();
				}else
					System.err.println("canceled");
			} catch (InterruptedException e) {}
		}

	}
}
