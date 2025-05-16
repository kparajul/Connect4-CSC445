package com.connect4.app.GameLogic;

import com.connect4.app.classes.Game;
import com.connect4.app.classes.Player;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private static final Map<String, Game> allSessions = new ConcurrentHashMap<>();
    private static final Map<WebSocket, Player> playerConnections = new ConcurrentHashMap<>();

    public static Game joinGame (String playerID, String gameID){
        Game game = null;
        if (allSessions.containsKey(gameID)){
            Game temp = allSessions.get(gameID);
            if ((temp != null) && (temp.getPlayer2Id() == null)){
                temp.setPlayer2Id(playerID);
                game = temp;
            }
        } else {
            //if game doesn't exist, you create a new game
            System.out.println("Game doesn't exist lol");
            game = createGame(playerID);
        }
        return game;
    }

    //you can create game with no player 2
    public static Game createGame(String playerID){
        String uniqueGameID = UUID.randomUUID().toString();
        Game game = new Game(playerID, null, uniqueGameID);
        game.setPlayerTurn(playerID);
        allSessions.put(uniqueGameID, game);
        return game;
    }

    public static Game reconnect(String playerID, String gameID){
        Game game = allSessions.get(gameID);
        if (game.getPlayer1Id().equals(playerID) || game.getPlayer2Id().equals(playerID)){
            return game;
        }
        return null;
    }

    public static void removeGame(String gameID){
        allSessions.remove(gameID);
    }

    public static Game getGame(String gameID){
        return allSessions.get(gameID);
    }

    public static void handleGameMessage(WebSocket connection, String message){
        String[] parse = message.split(" ");
        if(parse.length<2){
            connection.send("Invalid");
            return;
        }
        switch(parse[0]){
            case "NEW":
                handleNew(connection, parse);
                break;

            case "JOIN":
                handleJoin(connection, parse);
                return;
            case "RECONNECT":
                handleReconnect(connection, parse);
                return;
            case "MOVE":
                handleMove(connection, parse);
                return;
            default:
                connection.send("Wrong command lol");
        }
    }

    public static void handleNew(WebSocket connection, String[] parse){
        String playerName = parse[1];
        Game game = createGame(playerName);
        playerConnections.put(connection, new Player(playerName, game.getGameId()));
        connection.send("New Game created! Your game ID is:" + game.getGameId() + ". Share this gameID with a friend to play.");

    }
    public static void handleJoin(WebSocket connection, String[] parse){
        if (parse.length < 3){
            connection.send("Join command incorrect. Send: JOIN <playerName> <gameID>");
            return;
        }
        //check if they CAN join the game
        if (getGame(parse[2]).getPlayer2Id()!=null){
            connection.send("This game is full");
            return;
        }
        playerConnections.put(connection, new Player(parse[1], parse[2]));

        Game joinGame = joinGame(parse[1], parse[2]);
        if (joinGame != null) {
            connection.send("Joined game " + parse[2] + "with player name " + parse[1]);
        }

    }
    public static void handleReconnect(WebSocket connection, String[] parse){
        if(parse.length<3){
            connection.send("Reconnect command incorrect. Send: RECONNECT <playerName> <gameID>");
            return;
        }
        Game reGame = reconnect(parse[1], parse[2]);
        playerConnections.put(connection, new Player(parse[1], parse[2]));
        connection.send("Rejoined game " + reGame);

    }
    public static void handleMove(WebSocket connection, String[] parse){
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
        Game game = getGame(gameID);

        //just in case lol
        if(!(game.getPlayer1Id().equals(player) || game.getPlayer2Id().equals(player))){
            connection.send("Not your game");
            return;
        }

        //check turn before making a move
        if(!getGame(gameID).getPlayerTurn().equals(player)){
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
                removeGame(gameID);
                if (connection != null){
                    connection.send("You win, yay!! \n" + board);
                }
                if (opponentSocket != null){
                    opponentSocket.send(player+ " won the game Loser bwahahaha\n" + board);
                }
            } else if (draw) {
                removeGame(gameID);
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
    }

}