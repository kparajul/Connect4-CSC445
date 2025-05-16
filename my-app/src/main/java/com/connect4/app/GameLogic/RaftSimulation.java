package com.connect4.app.GameLogic;

import java.util.*;

public class RaftSimulation {

    public static void main(String[] args) throws Exception {
        List<RaftNode> cluster = new ArrayList<>();
        List<RaftWebSocketServer> servers = new ArrayList<>();

        // Create Raft nodes and their corresponding WebSocket servers
//        for (int i = 1; i <= 5; i++) {
//            RaftNode node = new RaftNode(i, servers); // Initialize with the list of servers
//            cluster.add(node);
//            RaftWebSocketServer server = new RaftWebSocketServer(26960 + i, node);
//            servers.add(server);
//            server.start();
//        }

        // Wait for the leader to be elected
        Thread.sleep(10000);

        // Now game clients can interact with the leader (via WebSocket connections)
//        GameClient client1 = new GameClient("ws://localhost:26960");
//        client1.sendMove("Player1 moved to A1");

        // Let the simulation run for a while
        Thread.sleep(5000);

        // Stop all servers
        for (RaftWebSocketServer server : servers) {
            server.stop();
        }
    }
}
