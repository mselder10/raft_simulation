package edu.duke.raft;

import java.util.Timer;

public class FollowerMode extends RaftMode {
	private static final int ELECTION_TIMER_ID = 0;
	private Timer electionTimer;

	public void go () {
		synchronized (mLock) {
			int term = mConfig.getCurrentTerm();
			System.out.println ("S" + 
					mID + 
					"." + 
					term + 
					": switched to follower mode.");
			resetElections();
		}
	}

	private void resetElections() {
		if (electionTimer != null) 
			electionTimer.cancel();

		long time = (long) (Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN)) + ELECTION_TIMEOUT_MIN;
		if(mConfig.getTimeoutOverride() > 0) {
			time = mConfig.getTimeoutOverride();
		}
		electionTimer = scheduleTimer(time, ELECTION_TIMER_ID);

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
			resetElections();
			int term = mConfig.getCurrentTerm();
			boolean self = (mConfig.getVotedFor() == 0 || mConfig.getVotedFor() == candidateID);
			if (candidateTerm >= term && lastLogTerm >= mLog.getLastTerm() && lastLogIndex >= mLog.getLastIndex() && self) {
				mConfig.setCurrentTerm(candidateTerm, candidateID);
				return 0;
			}
			else {
				mConfig.setCurrentTerm(candidateTerm, 0);
				return term;
			}
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
			resetElections();
			int term = mConfig.getCurrentTerm();
			mConfig.setCurrentTerm(leaderTerm, 0);
			int result = 0;
			int index = mLog.insert(entries, prevLogIndex, prevLogTerm);
			if (index == -1)
				result = term;
			else if(leaderCommit > mCommitIndex) {
				if(leaderCommit < index)
					mCommitIndex = leaderCommit;
				else
					mCommitIndex = index;
			}
			return result;
		}
	}  

	// @param id of the timer that timed out
	public void handleTimeout (int timerID) {
		synchronized (mLock) {
			if(timerID == ELECTION_TIMER_ID) {
				electionTimer.cancel();
				RaftServerImpl.setMode(new CandidateMode());
			}
		}
	}
}

