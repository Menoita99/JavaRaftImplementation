package com.raft.interpreter;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.raft.models.Entry;
import com.raft.models.Operation;
import com.raft.models.Task;
import com.raft.util.ThreadPool;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Interpreter {

	private ThreadPool pool = new ThreadPool(1);

//	private Binding binding = new Binding();
//	private GroovyShell shell = new GroovyShell(binding);

	private Map<String, Operation> operationsMap = new HashMap<String, Operation>();


	public void submit(List<Entry> entries) { 
		
		pool.submit(new Task(() -> this.execute(entries)));
	}
	
	
	
	private synchronized void execute(List<Entry> entries) {
		entries.sort((Entry o1, Entry o2)-> (int)o1.getIndex()- (int)o2.getIndex());
		for (Entry entry : entries) {
			Operation operation = null;
			try {
				//operation = new Operation(entry.getCommandID(), shell.evaluate(entry.getCommand()));
				operation = new Operation(entry.getCommandID(), "COMMAND EVALUATED");
			}catch (Exception e) {
				operation = new Operation(entry.getCommandID(),e);
				e.printStackTrace();
			}
			operationsMap.put(entry.getCommandID().split(":")[0], operation);
			notifyAll();
		}
	}

	
	
	
	

	public synchronized Object getCommandResult(String commandId,long timeOut) throws TimeoutException, InterruptedException {
		long startTime =System.currentTimeMillis();
		String clientIp =  commandId.split(":")[0];
		Operation op = operationsMap.getOrDefault(clientIp,null);
		while(op == null || !op.getOperationID().equals(commandId)) {
			wait(timeOut);
			if((System.currentTimeMillis()-startTime)>= timeOut)
				throw new TimeoutException();
			op = operationsMap.getOrDefault(clientIp,null);
		}
		return operationsMap.get(clientIp);
	}
}
