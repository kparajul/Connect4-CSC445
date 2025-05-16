package com.connect4.app.GameLogic;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.*;

public class RaftWebSocketServer extends WebSocketServer {

    private Map<String, WebSocket> serverConnections; // To store WebSocket connections with other Raft nodes
    private RaftNode raftNode;

    public RaftWebSocketServer(int port, RaftNode raftNode) {
        super(new InetSocketAddress(port));
        this.raftNode = raftNode;
        this.serverConnections = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        String remoteAddress = connection.getRemoteSocketAddress().toString();
        serverConnections.put(remoteAddress, connection);
        System.out.println("New connection from: " + remoteAddress);

        // After accepting the WebSocket client connection, the server can send messages like a heartbeat
        if (raftNode.getState() == RaftNode.State.LEADER) {
            raftNode.sendHeartbeats();
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
        // Handle messages from both game clients and other Raft nodes
        if (message.startsWith("heartbeat")) {
            handleHeartbeatMessage(message);
        } else if (message.startsWith("voteRequest")) {
            handleVoteRequest(message);
        } else if (message.startsWith("voteResponse")) {
            handleVoteResponse(message);
        }else if (message.startsWith("logEntry")){
            handleLogEntry(message);
        }else {
            // Handle game-specific messages (e.g., move or game creation)
            handleGameMessage(connection, message);
        }
    }

    @Override
    public void onError(WebSocket connection, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Raft WebSocket Server started...");
    }

    // Method to send heartbeats to all other Raft nodes
    public void sendHeartbeatsToFollowers() {
        for (WebSocket ws : serverConnections.values()) {
            String heartbeatMessage = "heartbeat:" + raftNode.getServerName() + ":" + raftNode.getCurrentTerm();
            ws.send(heartbeatMessage);
        }
    }

    // Handle heartbeat messages from other nodes
    private void handleHeartbeatMessage(String message) {
        String[] parts = message.split(":");
        int leaderName = Integer.parseInt(parts[1]);
        int term = Integer.parseInt(parts[2]);

        raftNode.receiveHeartbeat(leaderName, term); // Handle the heartbeat logic
    }

    // Handle vote request from other nodes
    private void handleVoteRequest(String message) {
        String[] parts = message.split(":");
        String candidate = parts[1];
        int term = Integer.parseInt(parts[2]);
        raftNode.requestVote(Integer.parseInt(candidate), term);
    }

    // Handle vote response from other nodes
    private void handleVoteResponse(String message) {
        String[] parts = message.split(":");
        String candidate = parts[1];
        raftNode.receiveVote(); // Process the vote
    }

    private void handleLogEntry(String message) {
        String parts = message.split(":")[1];
        raftNode.replicateLog(message);
    }

    // Handle game-related messages from WebSocket clients (players)
    private void handleGameMessage(WebSocket connection, String message) {
        // Example: Handle game moves or game creation
        if (raftNode.getState() == RaftNode.State.LEADER) {
            raftNode.appendEntry(message);  // Commit game actions to log
            GameManager.handleGameMessage(connection, message);
        } else {
            connection.send("Request denied, not the leader");
        }
    }

    public WebSocket getWebSocket() {
        return serverConnections.values().iterator().next();
    }

    public String getAddress1(){
        return this.getAddress().toString();
    }

    // Connect to other servers in the Raft cluster using WebSocket clients
//    public void connectToOtherServers(List<String> otherServers) {
//        for (String serverAddress : otherServers) {
//            try {
//                WebSocketClient client = new WebSocketClient(new URI("ws://" + serverAddress)) {
//                    @Override
//                    public void onOpen(ServerHandshake handshake) {
//                        System.out.println("Connected to server: " + serverAddress);
//                    }
//
//                    @Override
//                    public void onMessage(String message) {
//                        // Handle incoming messages from this server
//                    }
//
//                    @Override
//                    public void onClose(int code, String reason, boolean remote) {
//                        System.out.println("Connection closed to: " + serverAddress);
//                    }
//
//                    @Override
//                    public void onError(Exception ex) {
//                        ex.printStackTrace();
//                    }
//                };
//                client.connect();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
