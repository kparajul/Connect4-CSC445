//package com.connect4.app.GameLogic;
//
//import com.connect4.app.classes.Game;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class Server {
//    enum State { FOLLOWER, CANDIDATE, LEADER }
//
//    private int id;
//    private State state;
//    private int currentTerm;
//    private String votedFor;
//    private List<Game> log;
//    private List<Server> cluster;
//    private ScheduledExecutorService scheduler;
//    private int votesReceived;
//
//    public Server(int id, List<Server> cluster) {
//        this.id = id;
//        this.state = State.FOLLOWER;
//        this.currentTerm = 0;
//        this.votedFor = null;
//        this.log = new ArrayList<>();
//        this.cluster = cluster;
//        this.scheduler = Executors.newScheduledThreadPool(1);
//        startElectionTimer();
//    }
//
//    private void startElectionTimer() {
//        scheduler.schedule(this::startElection, getRandomTimeout(), TimeUnit.MILLISECONDS);
//    }
//
//    private int getRandomTimeout() {
//        return new Random().nextInt(3000) + 2000;  // 2-5 seconds
//    }
//
//    public synchronized void startElection() {
//        if (state == State.LEADER) return;  // Already a leader
//        state = State.CANDIDATE;
//        currentTerm++;
//        votedFor = String.valueOf(id);
//        votesReceived = 1;
//        System.out.println("Node " + id + " started election for term " + currentTerm);
//        for (Server node : cluster) {
//            if (node != this) {
//                node.requestVote(id, currentTerm);
//            }
//        }
//
//        scheduler.schedule(this::checkElectionResult, 1500, TimeUnit.MILLISECONDS);
//    }
//
//    private synchronized void checkElectionResult() {
//        if (state == State.CANDIDATE && votesReceived > cluster.size() / 2) {
//            becomeLeader();
//        } else {
//            // If the election failed, restart the election timer
//            state = State.FOLLOWER;
//            startElectionTimer();
//        }
//    }
//
//    public synchronized void requestVote(int candidateId, int term) {
//        if (term > currentTerm) {
//            currentTerm = term;
//            votedFor = -1;
//            state = State.FOLLOWER;
//        }
//
//        if (votedFor == -1 || votedFor == candidateId) {
//            votedFor = candidateId;
//            System.out.println("Node " + id + " voted for Node " + candidateId);
//            for (RaftNode node : cluster) {
//                if (node.id == candidateId) {
//                    node.receiveVote();
//                }
//            }
//        }
//    }
//
//    public synchronized void receiveVote() {
//        votesReceived++;
//        if (votesReceived > cluster.size() / 2) {
//            becomeLeader();
//        }
//    }
//
//    private synchronized void becomeLeader() {
//        state = State.LEADER;
//        System.out.println("Node " + id + " became leader for term " + currentTerm);
//        sendHeartbeats();
//    }
//
//    private void sendHeartbeats() {
//        scheduler.scheduleAtFixedRate(() -> {
//            if (state == State.LEADER) {
//                for (RaftNode node : cluster) {
//                    if (node != this) {
//                        node.receiveHeartbeat(id, currentTerm);
//                    }
//                }
//            }
//        }, 0, 1000, TimeUnit.MILLISECONDS);
//    }
//
//    public synchronized void receiveHeartbeat(int leaderId, int term) {
//        if (term >= currentTerm) {
//            state = State.FOLLOWER;
//            currentTerm = term;
//            votedFor = leaderId;
//        }
//    }
//
//    public synchronized void appendEntry(String entry) {
//        if (state == State.LEADER) {
//            log.add(entry);
//            for (RaftNode node : cluster) {
//                if (node != this) {
//                    node.replicateLog(entry);
//                }
//            }
//            System.out.println("Leader " + id + " committed log: " + log);
//        }
//    }
//
//    public synchronized void replicateLog(String entry) {
//        log.add(entry);
//        System.out.println("Node " + id + " replicated log: " + log);
//    }
//
//    public void stop() {
//        scheduler.shutdown();
//    }
//}
//}
