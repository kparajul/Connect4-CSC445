package com.connect4.app.GameLogic;

import com.connect4.app.classes.Game;
import com.connect4.app.classes.Moves;
import org.java_websocket.WebSocket;


import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RaftNode {
    enum State {LEADER, FOLLOWER, CANDIDATE}
    private int id;
    private State state;
    int currentTerm;
    int votedFor;
    ArrayList<String> log;
    private Map<String, WebSocket> serverConnections;
    private int votesReceived;
    private ScheduledExecutorService scheduler;



    // Constructor to initialize the Raft node and WebSocket connections
    public RaftNode(int id, List<RaftWebSocketServer> clusterServers) {
        this.id = id;
        this.state = State.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = -1;
        this.log = new ArrayList<>();
        this.serverConnections = new HashMap<>();
        this.votesReceived = 0;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        // Add WebSocket connections for all the other Raft servers
        for (RaftWebSocketServer server : clusterServers) {
            serverConnections.put(server.getAddress1(), server.getWebSocket());
        }
        startElectionTimer();
        // ... other initialization logic ...
    }

    private void startElectionTimer() {
        scheduler.schedule(this::startElection, getRandomTimeout(), TimeUnit.MILLISECONDS);
    }

    private int getRandomTimeout() {
        return new Random().nextInt(3000) + 2000;
    }

    private synchronized void startElection() {
        if(state == State.LEADER) {return;}
        state = State.CANDIDATE;
        currentTerm++;
        votedFor = id;
        votesReceived = 1;
        System.out.println("Server " + id + " started election for term " + currentTerm);
        for(WebSocket connection : serverConnections.values()) {
            connection.send("voteRequest:"+id+":"+currentTerm);
        }
        scheduler.schedule(this::checkElectionResult, 1500, TimeUnit.MILLISECONDS);
    }
    private synchronized void checkElectionResult() {
        if(state == State.CANDIDATE && votesReceived > serverConnections.size()/2){
            becomeLeader();
        }else{
            state = State.FOLLOWER;
            startElectionTimer();
        }
    }

    // Handle leader election, vote request, heartbeat, etc.
    public void handleVoteRequest(String candidate, int term) {
        // Handle vote request logic
    }

    public void receiveHeartbeat(int leaderName, int term) {
        // Handle heartbeat logic
        if(term >= currentTerm) {
            state = State.FOLLOWER;
            currentTerm = term;
            votedFor = leaderName;
        }
    }

    public synchronized void appendEntry(String entry) {
        // Append game log entry and replicate across servers
        if(state == State.LEADER){
            log.add(entry);
            System.out.println("Leader " + id + " committed log: " + log);
            for(WebSocket connection : serverConnections.values()) {
                connection.send("logEntry:" + entry);
            }
        }
    }


    public synchronized void replicateLog(String entry) {
        log.add(entry);
        System.out.println("Server "+id + " replicated log: " + log);
    }

    public  synchronized void receiveVote() {
        votesReceived++;
        if(votesReceived == serverConnections.size()/2){
            becomeLeader();
        }
    }

    public synchronized void requestVote(int candidateId, int term) {
        if(term > currentTerm){
            currentTerm = term;
            votedFor = -1;
            state = State.FOLLOWER;
        }
        if(votedFor == -1 && votedFor == candidateId){
            votedFor = candidateId;
            System.out.println("Server " + id + " voted for server " + candidateId);
            serverConnections.get("Server " + candidateId).send("voteResponse:"+candidateId);
        }
    }

    public synchronized void becomeLeader() {
        state = State.LEADER;
        System.out.println("Server " + id + " become leader for term "+ currentTerm);
        sendHeartbeats();
    }

    public void sendHeartbeats() {
        scheduler.scheduleAtFixedRate(()->{
            if(state == State.LEADER){
                for(WebSocket connection : serverConnections.values()) {
                    connection.send("heartbeat:"+id+":"+currentTerm);
                }
            }
        },0, 1000, TimeUnit.MILLISECONDS);
    }

    public State getState() {
        return state;
    }

    public String getServerName(){
    return "Server " + id;
    }

    public int getCurrentTerm(){
        return currentTerm;
    }

    public void stop(){
        scheduler.shutdown();
    }



    // Other Raft-related methods...
}
