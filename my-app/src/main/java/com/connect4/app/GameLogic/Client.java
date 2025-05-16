package com.connect4.app.GameLogic;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client extends WebSocketClient {
    private String currentServer;
    private final ScheduledExecutorService scheduler;
    private static final int MAX_RETRIES = 3;
    private int retryCount = 0;

    public Client(String serverAddress) throws URISyntaxException {
        super(new URI("ws://" + serverAddress + ":29690"));
        this.currentServer = serverAddress;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to server: " + currentServer);
        retryCount = 0; // Reset retry count on successful connection
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        
        if (message.startsWith("REDIRECT:")) {
            // Extract leader address and reconnect
            String leaderAddress = message.split(":")[1];
            System.out.println("Redirecting to leader: " + leaderAddress);
            currentServer = leaderAddress;
            close();
            reconnect();
        } else if (message.startsWith("CONNECTED:LEADER")) {
            System.out.println("Connected to leader");
        } else if (message.startsWith("ERROR:NO_LEADER")) {
            System.out.println("No leader available");
            handleNoLeader();
        } else if (message.startsWith("ACK:")) {
            System.out.println("Message acknowledged: " + message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + reason);
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            System.out.println("Retrying connection (" + retryCount + "/" + MAX_RETRIES + ")...");
            scheduler.schedule(this::reconnect, retryCount, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onError(Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
    }

   public void reconnect() {
        try {
            URI uri = new URI("ws://" + currentServer + ":26960");
            this.uri = uri;  // Update the URI directly
            this.connect();
        } catch (URISyntaxException e) {
            System.err.println("Invalid server address: " + e.getMessage());
        }
    }

    private void handleNoLeader() {
        System.out.println("No leader available, will retry connection");
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            scheduler.schedule(this::reconnect, retryCount, TimeUnit.SECONDS);
        }
    }

    public void sendCommand(String command) {
        if (this.isOpen()) {
            this.send(command);
        } else {
            System.err.println("WebSocket is not connected");
        }
    }

    public void stop() {
        scheduler.shutdown();
        this.close();
    }

    public static void main(String[] args) {
        try {
            // Create client with initial server address
            Client client = new Client("localhost");
            client.connect();

            // Keep the main thread alive
            Thread.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
} 