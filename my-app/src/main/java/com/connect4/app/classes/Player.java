package com.connect4.app.classes;

public class Player {
    private String playerID;
    private String gameID;

    //Constructor to initialize player with ID and moves
    public Player(String playerID, String gameID) {
        this.playerID = playerID;
        this.gameID = gameID; // Initialize moves object
    }

    public String getPlayerID(){
        return playerID;
    }
    public String getGameID(){
        return gameID;
    }
//    //Getters and Setters for player ID
//    public void setPlayerID(int playerID) {
//        this.playerID = playerID;
//    }
//    public int getPlayerID() {
//        return playerID;
//    }
//
//    //Getters and Setters for player moves
//    public void setPlayerMoves(Moves playerMoves) {
//        this.playerMoves = playerMoves;
//    }
//    public Moves getPlayerMoves() {
//        return playerMoves;
//    }
}
