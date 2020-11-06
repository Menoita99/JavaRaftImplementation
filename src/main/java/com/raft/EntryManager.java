package com.raft;


import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.raft.models.Address;
import com.raft.models.AppendResponse;
import com.raft.models.Entry;
import com.raft.models.Task;
import com.raft.state.Mode;
import com.raft.util.ThreadPool;

import lombok.Data;

@Data
public class EntryManager {

	private static final double MIN_TIME_OUT_MULTIPLIER  = 0.9;
	private Vector<Entry> entries = new Vector<>();
	private ThreadPool pool = new ThreadPool(1);
	private ExecutorService  executor;
	private Server server;
	private Resender resender;
	
	
	
	
	public EntryManager(Server server) {
		this.server = server;
		this.executor = Executors.newFixedThreadPool(server.getClusterArray().length);
		this.resender = new Resender();
	}

	
	
	
	
	public void submitEntry(Entry e) {
		if(e != null) 
			entries.add(e);
		pool.submit(new Task(this::sendAppendEntriesRequest));
	}



	

	public void sendAppendEntriesRequest() {
		if(server.getMode() == Mode.LEADER) {
			server.tryToConnect();
			try {
				List<Entry> entries = new ArrayList<>(this.entries);
				entries.sort((o1,o2) -> (int)o1.getIndex()-(int)o2.getIndex());
				
				server.getState().getLock().lock();
				long term = server.getState().getCurrentTerm();
				long prevLogIndex = entries.isEmpty() ? server.getState().getLastEntry().getIndex() : entries.get(0).getIndex()-1;
				long prevLogTerm = entries.isEmpty() ? server.getState().getLastEntry().getTerm() : entries.get(0).getTerm();
				long leaderCommit = server.getState().getCommitIndex();
				server.getState().getLock().unlock();

				List<Future<AppendResponse>> futures = new ArrayList<>();
				List<AppendResponse> responses = new ArrayList<>();

				for (FollowerBehaviour follower : server.getClusterFollow()) 
					if(follower != null) 
						futures.add(executor.submit(()->follower.appendEntries(term, server.getSelfId(), prevLogIndex, prevLogTerm,entries, leaderCommit)));
					else 
						futures.add(null);
				
				for (Future<AppendResponse> future : futures)  
					if(future != null)
						responses.add(future.get((long) (server.getMinTimeOut()*MIN_TIME_OUT_MULTIPLIER), TimeUnit.MILLISECONDS));
					else 
						responses.add(null);
				
				processResponses(responses,entries);
				
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
			}
		}
	}






	private void processResponses(List<AppendResponse> responses, List<Entry> entries) {
		int commited = 1;
		for (int i = 0; i < responses.size(); i++) {
			AppendResponse response = responses.get(i);
			if(response != null && !entries.isEmpty()) {
				entries.sort((o1,o2) -> (int)o1.getIndex()-(int)o2.getIndex());

				long appendedIndex = entries.get(entries.size()-1).getIndex();
				Address replicaAddress = server.getClusterArray()[i];
				
				if(response.isSuccess()) {
					server.getLeaderState().getNextIndex().put(replicaAddress, appendedIndex+1);
					server.getLeaderState().getMatchIndex().put(replicaAddress, appendedIndex);
					commited++;
				}else {
					server.getLeaderState().getNextIndex().put(replicaAddress, appendedIndex-1 < 0 ? 0 : appendedIndex-1);
					resender.submitResend(server.getClusterFollow().get(i),replicaAddress);
				}	
			}
		}

		if (commited > server.getClusterArray().length/2 && !entries.isEmpty()) {
			server.getState().setCommitIndex(entries.get(entries.size()-1).getIndex());
			this.entries.removeAll(entries);
			//Retry to commit
		}else if(!entries.isEmpty())
			pool.submit(new Task(this::sendAppendEntriesRequest));
	}



	
	
	/**
	 * 
	 * @author RuiMenoita
	 *
	 * Class responsible for recover log coherency of faulty followers
	 */
	private class Resender{
		
		private ThreadPool pool;
		private ExecutorService  executor;
		
		public Resender() {
			pool = new ThreadPool(server.getClusterArray().length);
			executor = Executors.newFixedThreadPool(server.getClusterArray().length);
		}
		
		public void submitResend(FollowerBehaviour follower, Address address) {
			pool.submit(new Task(()-> resendTo(follower, address)));
		}
		
		 
		private void resendTo(FollowerBehaviour follower, Address address) {
			long nextIndex = server.getLeaderState().getNextIndex().get(address);
			Entry prevEntry = server.getState().getEntry(nextIndex);
			List<Entry> entries = server.getState().getEntriesSince(nextIndex);
			
			server.getState().getLock().lock();
			long term = server.getState().getCurrentTerm();
			long prevLogIndex = prevEntry.getIndex();
			long prevLogTerm = entries.isEmpty() ? server.getState().getLastEntry().getTerm() : entries.get(0).getTerm();
			long leaderCommit = server.getState().getCommitIndex();
			server.getState().getLock().unlock();
			
			try {
				AppendResponse response = executor.submit(()->follower.appendEntries(term, server.getSelfId(), prevLogIndex, prevLogTerm,entries, leaderCommit)).get();
				if(!response.isSuccess()) {
					server.getLeaderState().getNextIndex().put(address, nextIndex-1);
					submitResend(follower, address);
				}
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}
	}
	
}
