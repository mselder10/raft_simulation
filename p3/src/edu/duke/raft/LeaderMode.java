package edu.duke.raft;


import java.util.*;

public class LeaderMode extends RaftMode {
	private static final int HEART_ID = 3;
	private int[] next;
	private Timer heartbeatTimer;

	public void go () {
		synchronized (mLock) {
			int term = mConfig.getCurrentTerm();
			System.out.println ("S" + 
					mID + 
					"." + 
					term + 
					": switched to leader mode.");
			next = new int[mConfig.getNumServers() +1];
			for (int i = 1; i <= mConfig.getNumServers(); i++) {
				next[i] = mLog.getLastIndex();
			}
			logRepair();
		}
	}
	private void logRepair() {
		while(true) {
			heartbeatTimer = scheduleTimer(HEARTBEAT_INTERVAL, HEART_ID);
			RaftResponses.setTerm(mConfig.getCurrentTerm());
			RaftResponses.clearAppendResponses(mConfig.getCurrentTerm());
			for(int i = 1; i <= mConfig.getNumServers(); i++) {
				int nextIndex = next[i];
				Entry[] newEntries = new Entry[1 + mLog.getLastIndex() - nextIndex];
				for (int j = nextIndex; j <= mLog.getLastIndex(); j++) {
					newEntries[j - nextIndex] = mLog.getEntry(j);
				}
				int prevIndex = nextIndex -1;
				int prev = -1;
				if (mLog.getEntry(prevIndex) != null) {
					prev = mLog.getEntry(prevIndex).term;
				}		
				remoteAppendEntries(i, mConfig.getCurrentTerm(), mID, prevIndex, prev, newEntries, mCommitIndex);
			}
			boolean done = true;
			int[] responses = RaftResponses.getAppendResponses(mConfig.getCurrentTerm());
			if(responses == null) {
				return;
			}
			for (int i = 1; i <= mConfig.getNumServers(); i++) {
				if(i != mID && responses[i] != 0) {
					done = false;
					if(next[i] > 0) {
						next[i]--;
					}
					else
						next[i] = 0;
					if(responses[i] > mConfig.getCurrentTerm()) {
						mConfig.setCurrentTerm(responses[i], 0);
						heartbeatTimer.cancel();
						RaftServerImpl.setMode(new FollowerMode());
						return;
					}
				}
			}
			if(done) {
				break;
			}
			heartbeatTimer.cancel();
		}
	}
	// @param candidate’s term
	// @param candidate requesting vote
	// @param index of candidate’s last log entry
	// @param term of candidate’s last log entry
	// @return 0, if server votes for candidate; otherwise, server's
	// current term
	public int requestVote (int candidateTerm,
			int candidateID,
			int lastLogIndex,
			int lastLogTerm) {
		synchronized (mLock) {
			int term = mConfig.getCurrentTerm();
			if(candidateTerm > term) {
				mConfig.setCurrentTerm(candidateTerm, 0);
				heartbeatTimer.cancel();
				RaftServerImpl.setMode(new FollowerMode());
			}
			return term;
		}
	}


	// @param leader’s term
	// @param current leader
	// @param index of log entry before entries to append
	// @param term of log entry before entries to append
	// @param entries to append (in order of 0 to append.length-1)
	// @param index of highest committed entry
	// @return 0, if server appended entries; otherwise, server's
	// current term
	public int appendEntries (int leaderTerm,
			int leaderID,
			int prevLogIndex,
			int prevLogTerm,
			Entry[] entries,
			int leaderCommit) {
		synchronized (mLock) {
			int term = mConfig.getCurrentTerm ();
			if(leaderTerm > term) {
				mConfig.setCurrentTerm(leaderTerm, 0);
				heartbeatTimer.cancel();
				RaftServerImpl.setMode(new FollowerMode());		
			}
			return term;

		}
	}

	// @param id of the timer that timed out
	public void handleTimeout (int timerID) {
		synchronized (mLock) {
			if(timerID == HEART_ID) {
				heartbeatTimer.cancel();
				logRepair();
			}
		}
	}
}
