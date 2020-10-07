package com.raft;

import com.raft.state.LeaderState;

import lombok.Getter;
import lombok.Setter;

/**
 * Class that represents the Leader Behaviour. 
 * This class also has the leader State object
 * @author RuiMenoita
 */
@Getter
@Setter
public abstract class Leader implements LeaderBehaviour{
	private LeaderState leaderState;
}
