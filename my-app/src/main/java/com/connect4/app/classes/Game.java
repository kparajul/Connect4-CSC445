package com.connect4.app.classes;

//Game is something we plan to serialize and save as cache.
public class Game {
	private String gameId;
	private String player1Id;
	private String player2Id;
	private Moves gameMoves;
	private String playerTurn;

	//Constructor to initialize game with player IDs and moves
	public Game(String player1Id, String player2Id, String gameId) {
		this.player1Id = player1Id;
		this.player2Id = player2Id;
		this.gameId = gameId;
		this.gameMoves = new Moves();
		this.playerTurn = player1Id;
	}

	//Getters and Setters for player 1's ID
	public void setPlayer1Id(String player1Id) {
		this.player1Id = player1Id;
	}
	public String getPlayer1Id() {
		return player1Id;
	}

	//Getters and Setters for player 2's ID
	public void setPlayer2Id(String player2Id) {
		this.player2Id = player2Id;
	}
	public String getPlayer2Id() {
		return player2Id;
	}

	//Getters and Setters for game ID
	public void setGameId(String gameId) {
		this.gameId = gameId;
	}


	public String getGameId() {
		return gameId;
	}

	public String getPlayerTurn(){
		return playerTurn;
	}

	public void setPlayerTurn(String player){
		this.playerTurn = player;
	}

	//Getters and Setters for game moves
	public void setGameMoves(Moves gameMoves) {
		this.gameMoves = gameMoves;
	}
	public Moves getGameMoves() {
		return gameMoves;
	}
}
