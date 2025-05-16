package com.connect4.app.GameLogic;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftNode {
    enum State {LEADER, FOLLOWER, CANDIDATE}
    
    // Node identification and state
    private final int id;
    private volatile State state;
    private final AtomicInteger currentTerm;
    private volatile int votedFor;
    private final List<LogEntry> log;
    private final Map<String, String> serverAddresses;
    private final ScheduledExecutorService scheduler;
    private final RaftUDPServer udpServer;
    
    // Leader state
    private final Map<String, Integer> nextIndex;
    private final Map<String, Integer> matchIndex;
    private volatile int commitIndex;
    private volatile int lastApplied;
    
    // Election state
    private volatile int votesReceived;
    private volatile long lastHeartbeatTime;
    
    // Constants
    private static final int HEARTBEAT_INTERVAL = 1000; // 1 second
    private static final int ELECTION_TIMEOUT_MIN = 2000; // 2 seconds
    private static final int ELECTION_TIMEOUT_MAX = 5000; // 5 seconds
    
    public static class LogEntry {
        private final int term;
        private final String command;
        
        public LogEntry(int term, String command) {
            this.term = term;
            this.command = command;
        }
        
        public int getTerm() { return term; }
        public String getCommand() { return command; }
    }
    
    public RaftNode(int id, List<String> clusterServers, int port) {
        this.id = id;
        this.state = State.FOLLOWER;
        this.currentTerm = new AtomicInteger(0);
        this.votedFor = -1;
        this.log = new ArrayList<>();
        this.serverAddresses = new HashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.nextIndex = new HashMap<>();
        this.matchIndex = new HashMap<>();
        this.commitIndex = 0;
        this.lastApplied = 0;
        this.votesReceived = 0;
        this.lastHeartbeatTime = System.currentTimeMillis();
        
        // Initialize UDP server with the specified port
        this.udpServer = new RaftUDPServer(port, this);
        
        // Initialize server addresses
        for (String server : clusterServers) {
            serverAddresses.put(server, server);
            nextIndex.put(server, 1);
            matchIndex.put(server, 0);
        }
    }

    public void start() {
        // Start the UDP server
        udpServer.start();
        
        // Connect to other servers
        List<String> otherServerAddresses = new ArrayList<>(serverAddresses.keySet());
        udpServer.connectToOtherServers(otherServerAddresses);
        
        // Start election timer
        startElectionTimer();
        
        System.out.println("RaftNode " + id + " started on port " + (26960));
        System.out.println("Connected to servers: " + otherServerAddresses);
    }
    
    private void startElectionTimer() {
        scheduler.schedule(this::checkElectionTimeout, getRandomTimeout(), TimeUnit.MILLISECONDS);
    }
    
    private int getRandomTimeout() {
        return new Random().nextInt(ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN) + ELECTION_TIMEOUT_MIN;
    }
    
    private synchronized void checkElectionTimeout() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastHeartbeatTime > ELECTION_TIMEOUT_MAX && state != State.LEADER) {
            startElection();
        }
        startElectionTimer();
    }
    
    private synchronized void startElection() {
        if (state == State.LEADER) return;
        
        state = State.CANDIDATE;
        currentTerm.incrementAndGet();
        votedFor = id;
        votesReceived = 1;
        lastHeartbeatTime = System.currentTimeMillis();
        
        System.out.println("Server " + id + " started election for term " + currentTerm.get());
        
        // Request votes from all servers
        for (String serverAddress : serverAddresses.keySet()) {
            // Format: voteRequest:candidateId:term:lastLogIndex:lastLogTerm
            String message = String.format("voteRequest:%d:%d:%d:%d", 
                id, currentTerm.get(), log.size() - 1, getLastLogTerm());
            System.out.println("Sending vote request to " + serverAddress + ": " + message);
            udpServer.sendMessage(message, serverAddress);
        }
        
        // Schedule election timeout
        scheduler.schedule(this::checkElectionResult, ELECTION_TIMEOUT_MIN, TimeUnit.MILLISECONDS);
    }
    
    private synchronized void checkElectionResult() {
        if (state == State.CANDIDATE && votesReceived >= (serverAddresses.size() + 1) / 2) {
            becomeLeader();
        } else {
            state = State.FOLLOWER;
            startElectionTimer();
        }
    }
    
    public synchronized void requestVote(int candidateId, int term, int lastLogIndex, int lastLogTerm) {
        System.out.println("Received vote request from " + candidateId + " for term " + term);
        
        boolean voteGranted = false;
        
        if (term > currentTerm.get()) {
            currentTerm.set(term);
            state = State.FOLLOWER;
            votedFor = -1;
        }
        
        if (term == currentTerm.get() && 
            (votedFor == -1 || votedFor == candidateId) &&
            isLogUpToDate(lastLogIndex, lastLogTerm)) {
            
            votedFor = candidateId;
            voteGranted = true;
            lastHeartbeatTime = System.currentTimeMillis();
            System.out.println("Granting vote to " + candidateId + " for term " + term);
        } else {
            System.out.println("Rejecting vote request from " + candidateId + " for term " + term);
        }
        
        if (voteGranted) {
            // Find the server address for the candidate
            String candidateAddress = null;
            for (String address : serverAddresses.keySet()) {
                if (address.startsWith("Server " + candidateId) || 
                    address.contains(":" + (26960 + candidateId))) {
                    candidateAddress = address;
                    break;
                }
            }
            
            if (candidateAddress != null) {
                // Format: voteResponse:voterId:term
                String message = String.format("voteResponse:%d:%d", id, currentTerm.get());
                System.out.println("Sending vote response to " + candidateAddress + ": " + message);
                udpServer.sendMessage(message, candidateAddress);
            } else {
                System.err.println("Could not find address for candidate " + candidateId);
            }
        }
    }
    
    private boolean isLogUpToDate(int lastLogIndex, int lastLogTerm) {
        if (log.isEmpty()) return true;
        
        LogEntry lastEntry = log.get(log.size() - 1);
        return lastLogTerm > lastEntry.getTerm() || 
               (lastLogTerm == lastEntry.getTerm() && lastLogIndex >= log.size() - 1);
    }
    
    public synchronized void receiveVote(int voterId, int term) {
        System.out.println("Received vote from " + voterId + " for term " + term);
        
        if (term == currentTerm.get() && state == State.CANDIDATE) {
            votesReceived++;
            System.out.println("Total votes received: " + votesReceived);
            if (votesReceived > (serverAddresses.size() + 1) / 2) {
                becomeLeader();
            }
        }
    }
    
    public synchronized void receiveHeartbeat(int leaderId, int term) {
        if (term >= currentTerm.get()) {
            currentTerm.set(term);
            state = State.FOLLOWER;
            votedFor = -1;
            lastHeartbeatTime = System.currentTimeMillis();
            
            // Reset election timer
            startElectionTimer();
        }
    }
    
    public synchronized void appendEntry(String entry) {
        if (state == State.LEADER) {
            LogEntry logEntry = new LogEntry(currentTerm.get(), entry);
            log.add(logEntry);
            
            // Update leader's matchIndex
            matchIndex.put("Server " + id, log.size() - 1);
            
            // Replicate to followers
            for (String server : serverAddresses.keySet()) {
                sendAppendEntries(server);
            }
            
            // Try to commit new entries
            updateCommitIndex();
        }
    }
    
    private void sendAppendEntries(String server) {
        int nextIdx = nextIndex.get(server);
        if (nextIdx < log.size()) {
            List<LogEntry> entries = log.subList(nextIdx, log.size());
            String entriesStr = entries.stream()
                .map(e -> e.getTerm() + ":" + e.getCommand())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
                
            String message = String.format("appendEntries:%d:%d:%d:%d:%s", 
                currentTerm.get(), id, nextIdx - 1, getLastLogTerm(), entriesStr);
            udpServer.sendMessage(message, server);
        }
    }
    
    public synchronized void receiveAppendEntries(int leaderId, int term, int prevLogIndex, 
                                                int prevLogTerm, List<LogEntry> entries) {
        if (term >= currentTerm.get()) {
            currentTerm.set(term);
            state = State.FOLLOWER;
            votedFor = -1;
            lastHeartbeatTime = System.currentTimeMillis();
            
            // Check if log is consistent
            if (prevLogIndex >= 0 && 
                (log.size() <= prevLogIndex || 
                 log.get(prevLogIndex).getTerm() != prevLogTerm)) {
                return;
            }
            
            // Append new entries
            if (entries != null && !entries.isEmpty()) {
                // Remove conflicting entries
                if (prevLogIndex + 1 < log.size()) {
                    log.subList(prevLogIndex + 1, log.size()).clear();
                }
                
                // Append new entries
                log.addAll(entries);
                
                // Update commit index
                updateCommitIndex();
            }
        }
    }
    
    private synchronized void updateCommitIndex() {
        if (state != State.LEADER) return;
        
        for (int n = commitIndex + 1; n < log.size(); n++) {
            if (log.get(n).getTerm() == currentTerm.get()) {
                int count = 1; // Count leader's own match
                for (int matchIdx : matchIndex.values()) {
                    if (matchIdx >= n) count++;
                }
                
                if (count > (serverAddresses.size() + 1) / 2) {
                    commitIndex = n;
                }
            }
        }
        
        // Apply committed entries
        while (lastApplied < commitIndex) {
            lastApplied++;
            applyLogEntry(log.get(lastApplied));
        }
    }
    
    private void applyLogEntry(LogEntry entry) {
        // Apply the log entry to the state machine
        System.out.println("Applying log entry: " + entry.getCommand());
        // TODO: Implement state machine application
    }
    
    private synchronized void becomeLeader() {
        state = State.LEADER;
        System.out.println("Server " + id + " became leader for term " + currentTerm.get());
        
        // Initialize leader state
        for (String server : serverAddresses.keySet()) {
            nextIndex.put(server, log.size());
            matchIndex.put(server, 0);
        }
        
        // Start sending heartbeats
        sendHeartbeats();
    }
    
    public void sendHeartbeats() {
        scheduler.scheduleAtFixedRate(() -> {
            if (state == State.LEADER) {
                for (String server : serverAddresses.keySet()) {
                    // Format: heartbeat:leaderId:term:commitIndex
                    String message = String.format("heartbeat:%d:%d:%d",
                            id, currentTerm.get(), commitIndex);
                    udpServer.sendMessage(message, server);
                }
            }
        }, 0, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    public State getState() {
        return state;
    }
    
    public String getServerName() {
        return "Server " + id;
    }
    
    public int getCurrentTerm() {
        return currentTerm.get();
    }
    
    private int getLastLogTerm() {
        return log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm();
    }
    
    public void stop() {
        scheduler.shutdown();
        udpServer.stop();
    }
}
