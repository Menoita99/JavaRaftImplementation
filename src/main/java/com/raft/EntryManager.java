package com.raft;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.raft.models.AppendResponse;
import com.raft.models.Entry;
import com.raft.models.Snapshot;
import com.raft.models.Task;
import com.raft.state.Mode;
import com.raft.state.ServerState;
import com.raft.util.ThreadPool;

import lombok.Data;

@Data
public class EntryManager {

	private static final double MIN_TIME_OUT_MULTIPLIER  = 0.9;
	private Vector<Entry> entries = new Vector<>();
	private ThreadPool pool = new ThreadPool(1);
	private ExecutorService  executor; 
	private Server server;
	private ServerState state;
	private Resender resender;




	public EntryManager(Server server) {
		this.server = server;
		this.state = server.getState();
		this.executor = Executors.newFixedThreadPool(server.getClusterArray().length);
		this.resender = new Resender(); 
	}





	public void submitEntry(Entry e) {
		if(e != null) 
			entries.add(e);
		pool.submit(new Task(this::sendAppendEntriesRequest));
	}






	public void sendAppendEntriesRequest(){
		if(server.getMode() == Mode.LEADER) {
			server.tryToConnect(false);
			List<Entry> entries = new ArrayList<>(this.entries);
			entries.sort((o1,o2) -> (int)o1.getIndex()-(int)o2.getIndex());

			state.getLock().lock();
			long term = state.getCurrentTerm();
			long prevLogIndex =	state.getLastAplied().getIndex();
			long prevLogTerm = state.getLastAplied().getTerm();
			long leaderCommit = state.getCommitIndex();
			state.getLock().unlock();

			int clusterLength = server.getClusterArray().length;
			@SuppressWarnings("unchecked")
			Future<AppendResponse>[] futures = (Future<AppendResponse>[]) Array.newInstance(Future.class, clusterLength);
			AppendResponse[] responses = new AppendResponse[clusterLength];

			int i = 0;
			for (FollowerBehaviour follower : server.getClusterFollowBehaviour()) {
				if(follower != null) 
					futures[i] = (executor.submit(()->follower.appendEntries(term, server.getSelfId(), prevLogIndex, prevLogTerm,entries, leaderCommit)));
				i++;
			}

			i = 0;
			for (Future<AppendResponse> future : futures) {
				if(future != null) 
					try {
						responses[i] = (future.get((long) (server.getMinTimeOut()*MIN_TIME_OUT_MULTIPLIER), TimeUnit.MILLISECONDS));
					}catch (Exception e) {
						System.err.println("Counld not send append entries to: "+server.getClusterArray()[i]);
						server.getClusterFollowBehaviour()[i] = null;
					}
				i++;
			}

			processResponses(responses,entries);
		}
	}






	private void processResponses(AppendResponse[] responses, List<Entry> entries) {
		int commited = 1;
		for (int i = 0; i < responses.length; i++) {
			AppendResponse response = responses[i];

			if(response != null) {
				if(!entries.isEmpty() &&response.isSuccess()) 
					commited++;
				if(!response.isSuccess()) 
					resender.submitResend(server.getClusterFollowBehaviour()[i],response);
				if(response.getTerm() > state.getCurrentTerm() || 
						(response.getTerm() == state.getCurrentTerm() && response.getLastEntry().getIndex() > state.getLastEntry().getIndex()))
					server.setMode(Mode.FOLLOWER);
			}
		}

		if (commited > server.getClusterArray().length/2 && !entries.isEmpty()) {
			state.setCommitIndex(entries.get(entries.size()-1).getIndex());
			this.entries.removeAll(entries);
			//Retry to commit
		}else if(!entries.isEmpty())
			pool.submit(new Task(this::sendAppendEntriesRequest));
	}





	/**
	 * 
	 * @author RuiMenoita
	 *
	 * Class responsible for recover log coherence of faulty followers
	 */
	private class Resender{

		private static final int CHUNCK_SIZE = 5000;
		private ExecutorService  executor;

		private HashMap<FollowerBehaviour,ThreadPool> threads = new HashMap<>();

		public Resender() {
			executor = Executors.newFixedThreadPool(server.getClusterArray().length);
		}

		public void submitResend(FollowerBehaviour follower, AppendResponse response) {
			if(threads.containsKey(follower)) {
				if(threads.get(follower).isWorkerAvailable())
					threads.get(follower).submit(new Task(()-> resendTo(follower, response)));
			}else {
				ThreadPool pool = new ThreadPool(1);
				pool.setDiscardLimit(1);
				threads.put(follower, pool);
				if(pool.isWorkerAvailable())
					pool.submit(new Task(()-> resendTo(follower, response)));
			}
		}


		//TODO FIND BETTER SOLUTION
		private LinkedList<Entry> resEntries;

		private void resendTo(FollowerBehaviour follower, AppendResponse response) {
			try {
				resEntries =  state.getEntriesSince(response.getLastEntry().getIndex()+1,CHUNCK_SIZE);
				System.out.println("SENDING "+response.getLastEntry().getIndex());
				state.getLock().lock();
				long term = state.getCurrentTerm();
				long prevLogIndex = response.getLastEntry().getIndex();
				long prevLogTerm = response.getLastEntry().getTerm();
				long leaderCommit = state.getCommitIndex();
				state.getLock().unlock();

				//send checkpoint
				if(resEntries == null) {
					ServerState snapState = Snapshot.recoverFromFile(server.getRoot());
					follower.InstallSnapshot(term, server.getSelfId(), new Snapshot(snapState));
					resEntries = new LinkedList<>();
				}else if(resEntries.size()< CHUNCK_SIZE)
					resEntries.addAll(entries);


				response = executor.submit(() ->follower.appendEntries(term, server.getSelfId(), prevLogIndex, prevLogTerm, resEntries, leaderCommit)).get(server.getTimeOutVote(), TimeUnit.MILLISECONDS);

				if(response.getTerm() > state.getCurrentTerm() || 
						(response.getTerm() == state.getCurrentTerm() && response.getLastEntry().getIndex() > state.getLastEntry().getIndex())) {
					System.out.println( response.getLastEntry());
					System.out.println(state.getLastEntry());
					server.setMode(Mode.FOLLOWER);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
