//package com.connect4.app.GameLogic;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class RaftCluster {
//    private final List<RaftNode> nodes;
//    private final List<RaftUDPServer> servers;
//    private static final int BASE_PORT = 26960;
//
//    public RaftCluster(int numNodes) {
//        this.nodes = new ArrayList<>();
//        this.servers = new ArrayList<>();
//
//        // Create server addresses
//        List<String> serverAddresses = new ArrayList<>();
//        for (int i = 0; i < numNodes; i++) {
//            serverAddresses.add("localhost:" + (BASE_PORT + i));
//        }
//
//        // Create nodes and their UDP servers
//        for (int i = 0; i < numNodes; i++) {
//            RaftNode node = new RaftNode(i + 1, serverAddresses);
//            nodes.add(node);
//        }
//    }
//
//    public void start() {
//        // Start all nodes
//        for (RaftNode node : nodes) {
//            node.start();
//        }
//    }
//
//    public void stop() {
//        // Stop all nodes
//        for (RaftNode node : nodes) {
//            node.stop();
//        }
//    }
//
//    public static void main(String[] args) {
//        // Create a cluster with 3 nodes
//        RaftCluster cluster = new RaftCluster(3);
//
//        // Start the cluster
//        cluster.start();
//
//        // Keep the main thread alive
//        try {
//            Thread.currentThread().join();
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }
//}