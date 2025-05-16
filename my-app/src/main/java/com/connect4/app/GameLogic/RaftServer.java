package com.connect4.app.GameLogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class RaftServer {
    private static final int BASE_PORT = 26960;
    private RaftNode node;
    private RaftUDPServer udpServer;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java RaftServer <nodeId> <port> [server1:port1 server2:port2 ...]");
            System.out.println("Example: java RaftServer 1 26960 pi.cs.oswego.edu:26960 localhost:26961");
            return;
        }

        try {
            // Parse command line arguments
            int nodeId = Integer.parseInt(args[0]);
            int port = Integer.parseInt(args[1]);
            
            // Get list of other servers
            List<String> otherServers = new ArrayList<>();
            if (args.length > 2) {
                otherServers.addAll(Arrays.asList(args).subList(2, args.length));
            }

            // Create and start the Raft node
            RaftNode node = new RaftNode(nodeId, otherServers, port);
            node.start();

            System.out.println("Raft Server started:");
            System.out.println("Node ID: " + nodeId);
            System.out.println("Port: " + port);
            System.out.println("Connected to servers: " + otherServers);

            // Keep the server running and handle user input
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\nCommands:");
                System.out.println("1. status - Show node status");
                System.out.println("2. exit - Stop the server");
                System.out.print("Enter command: ");

                String command = scanner.nextLine().trim().toLowerCase();
                
                switch (command) {
                    case "status":
                        printNodeStatus(node);
                        break;
                    case "exit":
                        System.out.println("Shutting down server...");
                        node.stop();
                        scanner.close();
                        return;
                    default:
                        System.out.println("Unknown command. Try 'status' or 'exit'");
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid node ID or port number");
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printNodeStatus(RaftNode node) {
        System.out.println("\nNode Status:");
        System.out.println("State: " + node.getState());
        System.out.println("Current Term: " + node.getCurrentTerm());
        System.out.println("Server Name: " + node.getServerName());
    }
} 