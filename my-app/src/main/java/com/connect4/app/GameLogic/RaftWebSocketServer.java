package com.connect4.app.GameLogic;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RaftWebSocketServer extends WebSocketServer {
    private final Map<String, WebSocket> serverConnections;
    private final RaftNode raftNode;
    private final Map<String, WebSocketClient> clientConnections;
    private final ScheduledExecutorService scheduler;
    private String leaderAddress;
    private volatile boolean isRunning;

    public RaftWebSocketServer(int port, RaftNode raftNode) {
        super(new InetSocketAddress(port));
        this.raftNode = raftNode;
        this.serverConnections = new ConcurrentHashMap<>();
        this.clientConnections = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.leaderAddress = "";
        this.isRunning = false;
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        String remoteAddress = connection.getRemoteSocketAddress().toString();
        serverConnections.put(remoteAddress, connection);
        System.out.println("New connection from: " + remoteAddress);
        if (!raftNode.getState().equals(RaftNode.State.LEADER)) {
            connection.send("Redirecting to " + leaderAddress);
        } else {
            connection.send("You are connected to leader. Ready to play.");
        }
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        String remoteAddress = connection.getRemoteSocketAddress().toString();
        serverConnections.remove(remoteAddress);
        System.out.println("Connection closed: " + remoteAddress);
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
//        if (message == null || message.isEmpty()) {
//            System.err.println("Received null or empty message");
//            return;
//        }
//        processMessage(message, connection);
        if (message.startsWith("gameRequest:")) {
            handleGameMessage(connection, message);
        } else {
            System.out.println("Received message: " + message);
        }
    }

//    private void processMessage(String message, WebSocket connection) {
//        try {
//            String[] parts = message.split(":");
//            String messageType = parts[0];
//
//            switch (messageType) {
//                case "heartbeat":
//                    if (validateMessage(parts, 4)) {
//                        handleHeartbeatMessage(parts);
//                    }
//                    break;
//                case "voteRequest":
//                    if (validateMessage(parts, 5)) {
//                        handleVoteRequest(parts);
//                    }
//                    break;
//                case "voteResponse":
//                    if (validateMessage(parts, 3)) {
//                        handleVoteResponse(parts);
//                    }
//                    break;
//                case "appendEntries":
//                    if (validateMessage(parts, 5)) {
//                        handleAppendEntries(parts);
//                    }
//                    break;
//                case "logEntry":
//                    if (validateMessage(parts, 2)) {
//                        handleLogEntry(parts);
//                    }
//                    break;
//                default:
//                    if (connection != null) {
//                        handleGameMessage(connection, message);
//                    } else {
//                        System.err.println("Received game message without connection context");
//                    }
//            }
//        } catch (Exception e) {
//            System.err.println("Error processing message: " + message);
//            e.printStackTrace();
//        }
//    }

    private boolean validateMessage(String[] parts, int expectedLength) {
        if (parts.length < expectedLength) {
            System.err.println("Invalid message format: expected " + expectedLength + " parts, got " + parts.length);
            return false;
        }
        return true;
    }

    private void handleHeartbeatMessage(String[] parts) {
        int leaderId = Integer.parseInt(parts[1]);
        int term = Integer.parseInt(parts[2]);
        int commitIndex = Integer.parseInt(parts[3]);
        raftNode.receiveHeartbeat(leaderId, term);
    }

    private void handleVoteRequest(String[] parts) {
        int candidateId = Integer.parseInt(parts[1]);
        int term = Integer.parseInt(parts[2]);
        int lastLogIndex = Integer.parseInt(parts[3]);
        int lastLogTerm = Integer.parseInt(parts[4]);
        raftNode.requestVote(candidateId, term, lastLogIndex, lastLogTerm);
    }

    private void handleVoteResponse(String[] parts) {
        int voterId = Integer.parseInt(parts[1]);
        int term = Integer.parseInt(parts[2]);
        raftNode.receiveVote(voterId, term);
    }

    private void handleAppendEntries(String[] parts) {
        int leaderId = Integer.parseInt(parts[1]);
        int term = Integer.parseInt(parts[2]);
        int prevLogIndex = Integer.parseInt(parts[3]);
        int prevLogTerm = Integer.parseInt(parts[4]);

        List<RaftNode.LogEntry> entries = new ArrayList<>();
        if (parts.length > 5) {
            String[] entryStrings = parts[5].split(",");
            for (String entryStr : entryStrings) {
                String[] entryParts = entryStr.split(":");
                int entryTerm = Integer.parseInt(entryParts[0]);
                String command = entryParts[1];
                entries.add(new RaftNode.LogEntry(entryTerm, command));
            }
        }

        raftNode.receiveAppendEntries(leaderId, term, prevLogIndex, prevLogTerm, entries);
    }

    private void handleLogEntry(String[] parts) {
        String entry = parts[1];
        raftNode.appendEntry(entry);
    }

    private void handleGameMessage(WebSocket connection, String message) {
        if (raftNode.getState() == RaftNode.State.LEADER) {
            raftNode.appendEntry(message);
            connection.send("Game action processed: " + message);
        } else {
            connection.send("You are connected to a follower. Redirecting to the leader...");
            connection.send("Redirecting to leader at: " + leaderAddress);
        }
    }

    @Override
    public void onError(WebSocket connection, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        isRunning = true;
        System.out.println("Raft WebSocket Server started on port " + getPort());
        startHealthCheck();
    }

    public WebSocket getWebSocket(String address) {
        return serverConnections.get(address);
    }

    public String getAddress1() {
        return this.getAddress().toString();
    }

//    public void connectToOtherServers(List<String> otherServers) {
//        for (String serverAddress : otherServers) {
//            connectWithRetry(serverAddress, 3);
//        }
//    }

//    private void connectWithRetry(String serverAddress, int maxRetries) {
//        int retries = 0;
//        while (retries < maxRetries && isRunning) {
//            try {
//                WebSocketClient client = createWebSocketClient(serverAddress);
//                client.connect();
//                return;
//            } catch (Exception e) {
//                retries++;
//                if (retries == maxRetries) {
//                    System.err.println("Failed to connect to " + serverAddress + " after " + maxRetries + " attempts");
//                    break;
//                }
//                try {
//                    Thread.sleep(1000 * retries); // Exponential backoff
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    break;
//                }
//            }
//        }
//    }

//    private WebSocketClient createWebSocketClient(String serverAddress) throws Exception {
//        return new WebSocketClient(new URI("ws://" + serverAddress)) {
//            @Override
//            public void onOpen(ServerHandshake handshake) {
//                System.out.println("Connected to server: " + serverAddress);
//                clientConnections.put(serverAddress, this);
//                System.out.println(clientConnections.get(serverAddress));
//            }
//
//            @Override
//            public void onMessage(String message) {
////                onMessage(null, message);
//            }
//
//            @Override
//            public void onClose(int code, String reason, boolean remote) {
//                System.out.println("Connection closed to: " + serverAddress);
//                clientConnections.remove(serverAddress);
//                // Attempt to reconnect if this was an unexpected closure
////                if (isRunning && remote) {
////                    reconnect(serverAddress);
////                }
//            }
//
//            @Override
//            public void onError(Exception ex) {
//                System.err.println("Error connecting to " + serverAddress + ": " + ex.getMessage());
//                ex.printStackTrace();
//            }
//        };
//    }
//
//    private void reconnect(String address) {
//        if (!isRunning) return;
//
//        System.out.println("Attempting to reconnect to: " + address);
//        connectWithRetry(address, 3);
//    }
//
//    public void stop() {
//        isRunning = false;
//
//        // Close all client connections
//        for (WebSocketClient client : clientConnections.values()) {
//            client.close();
//        }
//        clientConnections.clear();
//
////        // Close all server connections
////        for (WebSocket connection : serverConnections.values()) {
////            connection.close();
////        }
////        serverConnections.clear();
//
//        // Stop the server
//        try {
//            super.stop();
//        } catch (Exception e) {
//            System.err.println("Error stopping server: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }

    private void startHealthCheck() {
        scheduler.scheduleAtFixedRate(() -> {
            // Periodically check for the leader
            if (raftNode.getState() == RaftNode.State.LEADER) {
                leaderAddress = this.getAddress().toString(); // Update leader address
            }
            // Check for any closed WebSocket connections and attempt to reconnect
            for (Map.Entry<String, WebSocket> entry : serverConnections.entrySet()) {
                if (!entry.getValue().isOpen()) {
//                    reconnect(entry.getKey());
                }
            }
        }, 0, 5000, TimeUnit.MILLISECONDS);
    }

    public void setLeaderAddress(String leaderAddress) {
        this.leaderAddress = leaderAddress;
    }

    public String getLeaderAddress() {
        return this.leaderAddress;
    }
}