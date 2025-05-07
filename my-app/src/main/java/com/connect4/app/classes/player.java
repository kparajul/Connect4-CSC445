package com.connect4.app.classes;

public class player {
	private int playerID;
	private moves playerMoves;

	//Constructor to initialize player with ID and moves
	public player(int playerID) {
		this.playerID = playerID;
		this.playerMoves = new moves(); // Initialize moves object
	}

	//Getters and Setters for player ID
	public void setPlayerID(int playerID) {
		this.playerID = playerID;
	}
	public int getPlayerID() {
		return playerID;
	}

	//Getters and Setters for player moves
	public void setPlayerMoves(moves playerMoves) {
		this.playerMoves = playerMoves;
	}
	public moves getPlayerMoves() {
		return playerMoves;
	}
}
