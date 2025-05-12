package com.connect4.app.GameLogic;

import com.connect4.app.classes.Game;
import com.connect4.app.classes.Player;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

//Main server class that handles webSocket connections
public class Server extends WebSocketServer {
    private final boolean leader;

    //Keep track of sessions so users can make moves in a game without having to send game id and their name every single time
    private static final Map<WebSocket, Player> playerConnections = new ConcurrentHashMap<>();

    public Server(int port, boolean leader){
        super(new InetSocketAddress(port));
        this.leader = leader;
        RaftManager.electLeader(leader);
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake){
        //only leaders should accept connections
        if(!RaftManager.isLeader()){
            connection.send("Not a leader");
            connection.close();
            return;
        }
        connection.send("Welcome to Connect4. Send: NEW/JOIN/RECONNECT <playerName> <gameID>(if applicable) to start.");
    }

    @Override
    public void onClose(WebSocket connection, int i, String s, boolean b) {
        System.out.println("Connection closed " + s);
        //remove stale websockets
        playerConnections.remove(connection);
    }

    //You can create a game, join a game, reconnect or move. There are if else for each of these
    @Override
    public void onMessage(WebSocket connection, String message){
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

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        System.out.println("Error " + e.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Server is listening on port: " + getPort());
    }

    public static void main(String[] args) {
        int port = 3635;
        boolean leader = true;
        Server server = new Server(port, leader);
        server.start();
    }
}
