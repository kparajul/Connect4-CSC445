package com.connect4.app.GameLogic;

import com.connect4.app.classes.Game;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private static final Map<String, Game> allSessions = new ConcurrentHashMap<>();


    public static Game joinGame (String playerID, String gameID){
        Game game = null;
        if (allSessions.containsKey(gameID)){
            Game temp = allSessions.get(gameID);
            if ((temp != null) && (temp.getPlayer2Id() == null)){
                temp.setPlayer2Id(playerID);
                game = temp;
            }
        } else {
            System.out.println("Game doesn't exist lol");
            game = createGame(playerID);
        }
        return game;
    }

    public static Game createGame(String playerID){
        String uniqueGameID = UUID.randomUUID().toString();
        Game game = new Game(playerID, null, uniqueGameID);
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

}