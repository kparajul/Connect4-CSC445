package com.connect4.app.GameLogic;

import com.connect4.app.classes.Game;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

public class Server extends WebSocketServer {
    private final boolean leader;
    private static final Map<String, WebSocket> playerConnections = new ConcurrentHashMap<>();

    public Server(int port, boolean leader){
        super(new InetSocketAddress(port));
        this.leader = leader;
        RaftManager.electLeader(leader);
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake){
        if(!RaftManager.isLeader()){
            connection.send("Not a leader");
            connection.close();
            return;
        }
        connection.send("Welcome to Connect4. Send: NEW/JOIN/RECONNECT/MOVE <playerName> <gameID>(if applicable) <move>(if applicable) to start.");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        System.out.println("Connection closed " + s);
    }

    @Override
    public void onMessage(WebSocket connection, String message){
        String[] parse = message.split(" ");
        if(parse.length<2){
            connection.send("Invalid");
            return;
        }

        if(parse[0].equals("NEW")){
            String playerName = parse[1];
            playerConnections.put(playerName, connection);
            Game game = GameManager.createGame(playerName);
            connection.send("Game created! Your game ID is:" + game.getGameId());
        }else if (parse[0].equals("JOIN")){
            if (parse.length < 3){
                connection.send("Join command incorrect. Send: JOIN <playerName> <gameID>");
                return;
            }
            playerConnections.put(parse[1], connection);

            Game joinGame = GameManager.joinGame(parse[1], parse[2]);
            if (joinGame != null) {
                connection.send("Joined game " + parse[2] + "with player name " + parse[1]);
            }
        }else if(parse[0].equals("RECONNECT")){
            if(parse.length<3){
                connection.send("Reconnect command incorrect. Send: RECONNECT <playerName> <gameID>");
                return;
            }
            playerConnections.put(parse[1], connection);

            Game reGame = GameManager.reconnect(parse[1], parse[2]);
            connection.send("Rejoined game " + reGame);
        } else if (parse[0].equals("MOVE")) {
            if(parse.length<4){
                connection.send("Here is the command: MOVE <playerName> <GameID> <column>");
                return;
            }
            String gameID = parse[2];
            playerConnections.putIfAbsent(parse[1], connection);

            int col;
            try{
                col = Integer.parseInt(parse[3]);
            }catch (NumberFormatException e){
                connection.send("Move must be a number");
                return;
            }

            Game game = GameManager.getGame(parse[2]);
            String player = parse[1];

            if(!(game.getPlayer1Id().equals(player) || game.getPlayer2Id().equals(player))){
                connection.send("Not your game");
                return;
            }
            String opponent;
            synchronized (game) {
                if (!game.getPlayerTurn().equals(player)) {
                    connection.send("Not your turn");
                    return;
                }


                if (player.equals(game.getPlayer1Id())) {
                    opponent = game.getPlayer2Id();
                } else {
                    opponent = game.getPlayer1Id();
                }
                boolean moveMade = game.getGameMoves().makeMove(player, col);

                if(!moveMade){
                    connection.send("That's invalid bro");
                    return;
                }

                String board = game.getGameMoves().getBoardString();

                WebSocket playerSocket = playerConnections.get(player);
                WebSocket opponentSocket = playerConnections.get(opponent);

                boolean draw = false;
                boolean win = game.getGameMoves().checkWin(player);
                if(!win){
                    draw = game.getGameMoves().isBoardFull();
                }

                if (win){
                    GameManager.removeGame(gameID);
                    if (playerSocket != null){
                        playerSocket.send("You win, yay!! \n" + board);
                    }
                    if (opponentSocket != null){
                        opponentSocket.send(player+ " won the game Loser bwahahaha\n" + board);
                    }
                } else if (draw) {
                    GameManager.removeGame(gameID);
                    if (playerSocket != null){
                        playerSocket.send("It's a draw \n" + board);
                    }
                    if (opponentSocket != null){
                        opponentSocket.send( "It's a draw\n" + board);
                    }
                } else {
                    if (playerSocket != null){
                        playerSocket.send("You played column " + col + ". Board:\n" + board + "\nWaiting for " + opponent + ".");
                    }
                    if (opponentSocket != null){
                        opponentSocket.send(player+ "played column " + col + ". Board:\n" + board + "\nYour turnr ");
                    }
                }
            }

        } else {
            connection.send("Here is the format: COMMAND <playerName> <GameID>(optional)");
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.out.println("Error " + e.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Server is listening on port: " + getPort());
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        boolean leader = Boolean.parseBoolean(args[1]);
        Server server = new Server(port, leader);
        server.start();
    }
}
