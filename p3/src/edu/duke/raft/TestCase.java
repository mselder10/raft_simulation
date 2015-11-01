package edu.duke.raft;


public class TestCase {

  public TestCase (int numServers) {
  }

  // @return server id of server with highest term of all servers as
  // long as server is also in leader mode, 0 otherwise
  public int getLeader () {
    return 0;    
  }  

  // @param server id, between one and the number of servers
  // @return current term for passed-in server id
  public int getTerm (int server) {
    return 0;    
  }

  // @param server id, between one and the number of servers
  public void failServer (int server) {
  }

  // @param server id, between one and the number of servers  
  public void startServer (int server) {
  }  

  // @param server id, between one and the number of servers
  // @param server id, between one and the number of servers
  public void sendVoteRequest (int caller, int callee) {
  }

  // @param server id, between one and the number of servers
  // @param server id, between one and the number of servers
  public void sendAppendRequest (int caller, int callee) {
  }  

  // @param server id, between one and the number of servers
  public void elapseElectionTimeout (int server) {
  }

  // @param server id, between one and the number of servers
  public void checkResponses (int server) {
  }

  // @param server id, between one and the number of servers
  // @param server id, between one and the number of servers  
  public void setVoteResponse (int caller, int callee) {
  }

  // @param server id, between one and the number of servers
  // @param server id, between one and the number of servers  
  public void setAppendResponse (int caller, int callee) {
  }

  // @return -1 if first server is more up to date, 0 if logs are
  // equal, 1 if second server is more up to date
  public int compareLogs (int firstServer, int secondServer) {
    return 0;
  }  
}
