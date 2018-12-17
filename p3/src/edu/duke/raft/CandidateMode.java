package edu.duke.raft;

import java.util.Timer;


public class CandidateMode extends RaftMode {
	private Timer electionTimer;
	private Timer pollTimer;
	private static int ELECTION_TIMER_ID = 1;
	private static int POLL_TIMER_ID = 2;
	private static int POLL_FREQUENCY = 10;

	public void go () {
		synchronized (mLock) {
			int term = mConfig.getCurrentTerm();
			term ++;
			mConfig.setCurrentTerm(term, mID);
			System.out.println ("S" + 
					mID + 
					"." + 
					term + 
					": switched to candidate mode.");
			holdElections();
		}
	}

	private void holdElections() {
		if (electionTimer != null) 
			electionTimer.cancel();
		int term = mConfig.getCurrentTerm();

		long time = (long) (Math.random() * (ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN)) + ELECTION_TIMEOUT_MIN;
		if(mConfig.getTimeoutOverride() == -1) {
			time = mConfig.getTimeoutOverride();
		}
		electionTimer = scheduleTimer(time, ELECTION_TIMER_ID);

		RaftResponses.setTerm(term);
		RaftResponses.clearVotes(term);

		for(int i = 1; i <= mConfig.getNumServers(); i++) {
			remoteRequestVote(i, term, mID, mLog.getLastIndex(), mLog.getLastTerm());
		}

		pollTimer = scheduleTimer(POLL_FREQUENCY, POLL_TIMER_ID);
	}		

	private void timerStop() {
		electionTimer.cancel();
		pollTimer.cancel();
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
			int result = term;
			if(candidateID == mID) {
				result = 0;
				return result;
			}
			if(candidateTerm <= term) {
				return result;
			} else {
				mConfig.setCurrentTerm(candidateTerm, 0);
				timerStop();
				RaftServerImpl.setMode(new FollowerMode());
			}
			return result;

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
			int term = mConfig.getCurrentTerm();
			if (leaderTerm >= term) {
				mConfig.setCurrentTerm(leaderTerm, 0);
				timerStop();
				RaftServerImpl.setMode(new FollowerMode());
			}
			return term;
		}
	}


	// @param id of the timer that timed out
	public void handleTimeout (int timerID) {
		synchronized (mLock) {
			if(timerID == ELECTION_TIMER_ID) {
				timerStop();
				mConfig.setCurrentTerm(mConfig.getCurrentTerm() +1, mID);
				holdElections();
			} else if(timerID == POLL_TIMER_ID) {
				int[] totalVotes;
				pollTimer.cancel();
				if(RaftResponses.getVotes(mConfig.getCurrentTerm()) == null) {
					pollTimer = scheduleTimer(POLL_FREQUENCY, POLL_TIMER_ID);
					return;
				}
				else {
					totalVotes = RaftResponses.getVotes(mConfig.getCurrentTerm());
				}

				int yesVotes = 0;
				for (int i = 1; i <= mConfig.getNumServers(); i++){
					if(totalVotes[i] == 0) {
						yesVotes++;
					}
					if(totalVotes[i] > mConfig.getCurrentTerm()) {
						electionTimer.cancel();
						mConfig.setCurrentTerm(totalVotes[i], 0);
						RaftServerImpl.setMode(new FollowerMode());
						return;
					}
				}
				if(yesVotes >= (mConfig.getNumServers() + 1)/2) {
					electionTimer.cancel();
					RaftServerImpl.setMode(new LeaderMode());
				}
				else {
					pollTimer = scheduleTimer(POLL_FREQUENCY, POLL_TIMER_ID);
				}
			}

		}
	}
}