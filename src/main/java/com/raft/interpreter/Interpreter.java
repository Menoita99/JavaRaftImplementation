package com.raft.interpreter;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.raft.models.Entry;
import com.raft.models.Operation;
import com.raft.models.Task;
import com.raft.util.ThreadPool;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Interpreter implements Serializable{

	private static final long serialVersionUID = 1L;
	private ThreadPool pool = new ThreadPool(1);

	private Map<String, Operation> operationsMap = new HashMap<String, Operation>();
	// Keys and values should be strings
	private HashMap<String, String> keyStore = new HashMap<String, String>();


	public void submit(List<Entry> entries) { 
		pool.submit(new Task(() -> this.execute(entries)));
	}



	private synchronized void execute(List<Entry> entries) {
		entries.sort((Entry o1, Entry o2)-> (int)o1.getIndex()- (int)o2.getIndex());
		for (Entry entry : entries) {
			Operation operation = null;
			try {
				operation = new Operation(entry.getCommandID(), shell(entry.getCommand()));
			}catch (Exception e) {
				operation = new Operation(entry.getCommandID(),e);
				e.printStackTrace();
			}
			operationsMap.put(entry.getCommandID().split(":")[0], operation);
			notifyAll();
		}
	}


	private Object shell(String command) {
		String[] v = command.split(":");
		if(v.length>=2)
			switch(v[0]) {
			case "put":
				keyStore.put(v[1], v[2]);
				return "Added: " + v[1] + ":" + v[2];
			case "get":
				return keyStore.get(v[1]);
			case "del":
				keyStore.remove(v[1], v[2]);
				return "Removed:" + v[1] + ":" + v[2];
			case "lis":
				String lis ="";
				for(java.util.Map.Entry<String, String> key: keyStore.entrySet()) 
					lis += key.getKey() + ":" + key.getValue()+"\n";
				return lis;
			case "cas":
				// v  1, 2,   3
				//cas(K,Vold,Vnew)
				//				x=get(K); 
				//				if(x==Vold) 
				//					put(K,Vnew); 
				//				return x;
				String x = keyStore.get(v[1]);
				if(x==v[2]) {
					keyStore.put(v[1], v[3]);
				}
				return x;
			}
		return "something wrong is not right";
	}



	public synchronized Object getCommandResult(String commandId,long timeOut) throws TimeoutException, InterruptedException {
		long startTime =System.currentTimeMillis();
		String clientIp =  commandId.split(":")[0];
		Operation op = operationsMap.getOrDefault(clientIp,null);
		while(op == null || !op.getOperationID().equals(commandId)) {
			wait(timeOut);
			if((System.currentTimeMillis()-startTime)>= timeOut && timeOut>0)
				throw new TimeoutException();
			op = operationsMap.getOrDefault(clientIp,null);
		}
		return operationsMap.get(clientIp);
	}
}
