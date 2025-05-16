package com.connect4.app.GameLogic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class RaftUDPServer {
    private final int port;
    private final RaftNode raftNode;
    private final Map<String, InetAddress> serverAddresses;
    private final Map<Integer, Message> pendingMessages;
    private final ScheduledExecutorService scheduler;
    private final AtomicInteger messageId;
    private volatile boolean isRunning;
    private DatagramSocket socket;
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_MS = 1000;
    private static final int MAX_PACKET_SIZE = 1024;

    public RaftUDPServer(int port, RaftNode raftNode) {
        this.port = port;
        this.raftNode = raftNode;
        this.serverAddresses = new ConcurrentHashMap<>();
        this.pendingMessages = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.messageId = new AtomicInteger(0);
        this.isRunning = false;
    }

    public void start() {
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(TIMEOUT_MS);
            socket.setBroadcast(true);
            isRunning = true;
            
            scheduler.submit(this::receiveMessages);
            scheduler.scheduleAtFixedRate(this::checkRetransmissions, 0, 500, TimeUnit.MILLISECONDS);
            
            System.out.println("Raft UDP Server started on port " + port);
        } catch (IOException e) {
            System.err.println("Failed to start UDP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        isRunning = false;
        if (socket != null) {
            socket.close();
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void connectToOtherServers(List<String> otherServers) {
        for (String serverAddress : otherServers) {
            try {
                String[] parts = serverAddress.split(":");
                if (parts.length != 2) {
                    System.err.println("Invalid server address format: " + serverAddress);
                    continue;
                }
                
                InetAddress address = InetAddress.getByName(parts[0]);
                serverAddresses.put(serverAddress, address);
                System.out.println("Added server: " + serverAddress);
            } catch (Exception e) {
                System.err.println("Failed to add server " + serverAddress + ": " + e.getMessage());
            }
        }
    }

    private void receiveMessages() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (isRunning) {
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received message from " + packet.getAddress() + ":" + packet.getPort() + ": " + message);
                processMessage(message, packet.getAddress(), packet.getPort());
            } catch (SocketTimeoutException e) {
                continue;
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Error receiving message: " + e.getMessage());
                }
            }
        }
    }

    private void processMessage(String message, InetAddress senderAddress, int senderPort) {
        try {
            String[] parts = message.split(":");
            String messageType = parts[0];

            if (messageType.equals("ACK")) {
                handleAcknowledgment(parts);
                return;
            }

            int msgId = Integer.parseInt(parts[1]);
            sendAcknowledgment(msgId, senderAddress, senderPort);

            switch (messageType) {
                case "heartbeat":
                    if (validateMessage(parts, 4)) {
                        System.out.println("Received heartbeat from " + senderAddress + ":" + senderPort);
                        handleHeartbeatMessage(parts);
                    }
                    break;
                case "voteRequest":
                    if (validateMessage(parts, 5)) {
                        System.out.println("Received vote request from " + senderAddress + ":" + senderPort);
                        handleVoteRequest(parts);
                    }
                    break;
                case "voteResponse":
                    if (validateMessage(parts, 3)) {
                        System.out.println("Received vote response from " + senderAddress + ":" + senderPort);
                        handleVoteResponse(parts);
                    }
                    break;
                case "appendEntries":
                    if (validateMessage(parts, 5)) {
                        System.out.println("Received append entries from " + senderAddress + ":" + senderPort);
                        handleAppendEntries(parts);
                    }
                    break;
                case "logEntry":
                    if (validateMessage(parts, 2)) {
                        System.out.println("Received log entry from " + senderAddress + ":" + senderPort);
                        handleLogEntry(parts);
                    }
                    break;
                default:
                    System.err.println("Unknown message type: " + messageType);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleAcknowledgment(String[] parts) {
        int msgId = Integer.parseInt(parts[1]);
        Message message = pendingMessages.remove(msgId);
        if (message != null) {
            System.out.println("Received ACK for message " + msgId + " from " + message.address + ":" + message.port);
        } else {
            System.out.println("Received ACK for unknown message " + msgId);
        }
    }

    private void sendAcknowledgment(int msgId, InetAddress address, int port) {
        String ackMessage = "ACK:" + msgId;
        try {
            byte[] data = ackMessage.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            System.out.println("Sent ACK for message " + msgId + " to " + address + ":" + port);
        } catch (IOException e) {
            System.err.println("Error sending ACK: " + e.getMessage());
        }
    }

    private void checkRetransmissions() {
        if (!isRunning) return;

        long currentTime = System.currentTimeMillis();
        pendingMessages.forEach((msgId, message) -> {
            if (currentTime - message.timestamp > TIMEOUT_MS) {
                if (message.retries < MAX_RETRIES) {
                    retransmitMessage(message);
                } else {
                    System.err.println("Message " + msgId + " failed after " + MAX_RETRIES + " retries");
                    pendingMessages.remove(msgId);
                }
            }
        });
    }

    private void retransmitMessage(Message message) {
        try {
            byte[] data = message.content.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, message.address, message.port);
            socket.send(packet);
            message.retries++;
            message.timestamp = System.currentTimeMillis();
            System.out.println("Retransmitting message " + message.id + " (attempt " + message.retries + ")");
        } catch (IOException e) {
            System.err.println("Error retransmitting message: " + e.getMessage());
        }
    }

    public void sendMessage(String message, String serverAddress) {
        if (!isRunning) return;

        try {
            String[] parts = serverAddress.split(":");
            if (parts.length != 2) {
                System.err.println("Invalid server address format: " + serverAddress);
                return;
            }

            InetAddress address = InetAddress.getByName(parts[0]);
            int serverPort = Integer.parseInt(parts[1]);
            
            int msgId = messageId.incrementAndGet();
//            String messageWithId = msgId + ":" + message;
            
            Message pendingMessage = new Message(msgId, message, address, serverPort);
            pendingMessages.put(msgId, pendingMessage);
            
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, address, serverPort);
            socket.send(packet);
            System.out.println("Sent message " + msgId + " to " + serverAddress + ": " + message);
        } catch (IOException e) {
            System.err.println("Error sending message to " + serverAddress + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number in address: " + serverAddress);
        }
    }

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

    private static class Message {
        final int id;
        final String content;
        final InetAddress address;
        final int port;
        int retries;
        long timestamp;

        Message(int id, String content, InetAddress address, int port) {
            this.id = id;
            this.content = content;
            this.address = address;
            this.port = port;
            this.retries = 0;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
