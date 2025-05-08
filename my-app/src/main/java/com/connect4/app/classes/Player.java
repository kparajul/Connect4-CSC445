package com.connect4.app.classes;

public class Player {
    private int playerID;
    private Moves playerMoves;

    //Constructor to initialize player with ID and moves
    public Player(int playerID) {
        this.playerID = playerID;
        this.playerMoves = new Moves(); // Initialize moves object
    }

    //Getters and Setters for player ID
    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }
    public int getPlayerID() {
        return playerID;
    }

    //Getters and Setters for player moves
    public void setPlayerMoves(Moves playerMoves) {
        this.playerMoves = playerMoves;
    }
    public Moves getPlayerMoves() {
        return playerMoves;
    }
}
