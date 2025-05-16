package com.connect4.app.GameLogic;

import com.connect4.app.classes.Game;
import com.connect4.app.classes.Player;
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

import static com.connect4.app.GameLogic.GameManager.playerConnections;
import static com.connect4.app.GameLogic.GameManager.reconnect;

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
            connection.send("REDIRECT " + leaderAddress);
        } else {
            connection.send("You are connected to leader. Ready to play.");
            connection.send("CONNECTED:LEADER");
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
//        if (message.startsWith("gameRequest:")) {
//            handleGameMessage(connection, message);
//            connection.send("ACK:" + message);
//        } else {
//            System.out.println("Received message: " + message);
//        }


        String[] parse = message.split(" ");
        if(parse.length<2){
            connection.send("Invalid");
            return;
        }

        ///////////////////////////////////////////////////////////////////
        //creating a new game is NEW <playerName>
        if(parse[0].equals("NEW")){
            String playerName = parse[1];
            Game game = GameManager.createGame(playerName);
            playerConnections.put(connection, new Player(playerName, game.getGameId()));

            connection.send("New Game created! Your game ID is:" + game.getGameId() + ". Share this gameID with a friend to play.");

            ///////////////////////////////////////////////////////////////////
            //joining someone else's game is JOIN <playerName> <gameID>
        }else if (parse[0].equals("JOIN")){
            if (parse.length < 3){
                connection.send("Join command incorrect. Send: JOIN <playerName> <gameID>");
                return;
            }
            //check if they CAN join the game
            if (GameManager.getGame(parse[2]).getPlayer2Id()!=null){
                connection.send("This game is full");
                return;
            }
            playerConnections.put(connection, new Player(parse[1], parse[2]));

            Game joinGame = GameManager.joinGame(parse[1], parse[2]);
            if (joinGame != null) {
                connection.send("Joined game " + parse[2] + "with player name " + parse[1]);
            }

            ///////////////////////////////////////////////////////////////////
            //Reconnect a previous game. RECONNECT <playerName> <gameID>
        }else if(parse[0].equals("RECONNECT")){
            if(parse.length<3){
                connection.send("Reconnect command incorrect. Send: RECONNECT <playerName> <gameID>");
                return;
            }
            Game reGame = GameManager.reconnect(parse[1], parse[2]);
            playerConnections.put(connection, new Player(parse[1], parse[2]));
            connection.send("Rejoined game " + reGame);

            //////////////////////////////////////////////////////////////////////
            //Move command valid when they are in playerConnections
        } else if (parse[0].equals("MOVE")) {
            if(parse.length!= 2){
                connection.send("You can move when you're in a game. Here is the command: MOVE <column>");
                return;
            }
            //get their session using the shared hashmap
            Player session = playerConnections.get(connection);
            if(session == null){
                connection.send("You're not currently in a game");
                return;
            }

            //Once they are connected once, they don't have to input their player name and game ID
            String player = session.getPlayerID();
            String gameID = session.getGameID();
            Game game = GameManager.getGame(gameID);

            //just in case lol
            if(!(game.getPlayer1Id().equals(player) || game.getPlayer2Id().equals(player))){
                connection.send("Not your game");
                return;
            }

            //check turn before making a move
            if(!GameManager.getGame(gameID).getPlayerTurn().equals(player)){
                connection.send("Not your turn");
                return;
            }
            int col;
            try{
                col = Integer.parseInt(parse[1]);
            }catch (NumberFormatException e){
                connection.send("Move must be a number");
                return;
            }
            String opponent;
            if (player.equals(game.getPlayer1Id())) {
                opponent = game.getPlayer2Id();
            } else {
                opponent = game.getPlayer1Id();
            }
            synchronized (game) {

                //make a move
                boolean moveMade = game.getGameMoves().makeMove(player, col);
                game.setPlayerTurn(opponent);

                if(!moveMade){
                    connection.send("That's invalid bro");
                    return;
                }

                String board = game.getGameMoves().getBoardString();

                WebSocket opponentSocket = null;

                for(Map.Entry<WebSocket, Player> key: playerConnections.entrySet()){
                    if(key.getValue().getPlayerID().equals(opponent)){
                        opponentSocket = key.getKey();
                        break;
                    }
                }
                boolean draw = false;
                boolean win = game.getGameMoves().checkWin(player);
                if(!win){
                    draw = game.getGameMoves().isBoardFull();
                }

                if (win){
                    GameManager.removeGame(gameID);
                    if (connection != null){
                        connection.send("You win, yay!! \n" + board);
                    }
                    if (opponentSocket != null){
                        opponentSocket.send(player+ " won the game Loser bwahahaha\n" + board);
                    }
                } else if (draw) {
                    GameManager.removeGame(gameID);
                    if (connection != null){
                        connection.send("It's a draw \n" + board);
                    }
                    if (opponentSocket != null){
                        opponentSocket.send( "It's a draw\n" + board);
                    }
                } else {
                    if (connection != null){
                        connection.send("You played column " + col + ". Board:\n" + board + "\nWaiting for " + opponent + ".");
                    }
                    if (opponentSocket != null){
                        opponentSocket.send(player+ "played column " + col + ". Board:\n" + board + "\nYour turn");
                    }
                }
            }

        } else {
            connection.send("Wrong command");
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
            connection.send("CONNECTED:LEADER");
        } else {
            connection.send("You are connected to a follower. Redirecting to the leader...");
            connection.send("REDIRECT" + leaderAddress);
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
            for (Map.Entry<String, WebSocket> entry : serverConnections.entrySet()) {
                if (!entry.getValue().isOpen()) {
                    System.out.println("Connection to " + entry.getKey() + " is closed, attempting to reconnect");
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