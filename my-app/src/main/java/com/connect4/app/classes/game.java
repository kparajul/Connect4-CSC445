package com.connect4.app.classes;

public class game {
	private int gameId;
	private int player1Id;
	private int player2Id;
	private moves gameMoves;

	//Constructor to initialize game with player IDs and moves
	public game(int player1Id, int player2Id) {
		this.player1Id = player1Id;
		this.player2Id = player2Id;
		this.gameMoves = new moves(); // Initialize moves object
	}

	//Getters and Setters for player 1's ID
	public void setPlayer1Id(int player1Id) {
		this.player1Id = player1Id;
	}
	public int getPlayer1Id() {
		return player1Id;
	}

	//Getters and Setters for player 2's ID
	public void setPlayer2Id(int player2Id) {
		this.player2Id = player2Id;
	}
	public int getPlayer2Id() {
		return player2Id;
	}

	//Getters and Setters for game ID
	public void setGameId(int gameId) {
		this.gameId = gameId;
	}
	public int getGameId() {
		return gameId;
	}

	//Getters and Setters for game moves
	public void setGameMoves(moves gameMoves) {
		this.gameMoves = gameMoves;
	}
	public moves getGameMoves() {
		return gameMoves;
	}
}
