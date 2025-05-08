package com.connect4.app.classes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Moves {
    // Instance variables
    private Map<String, Integer> playerMoves;

    // Constructor to initialize
    public Moves() {
        this.playerMoves = Collections.synchronizedMap(new HashMap<String, Integer>());
    }

    // Getters and setters for playerMoves
    public Map<String, Integer> getPlayerMoves() {
        return playerMoves;
    }
    public void setPlayerMoves(HashMap<String, Integer> playerMoves) {
        this.playerMoves = playerMoves;
    }

    // Getters and setters for a specific player's move
    public Integer getMoveByPlayerId(String playerId) {
        return playerMoves.get(playerId);
    }
    public void setMoveForPlayer(String playerId, Integer position) {
        playerMoves.put(playerId, position);
    }
}
