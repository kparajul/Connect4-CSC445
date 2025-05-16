package com.connect4.app.classes;
import java.util.*;
import java.util.stream.Collectors;

// We are recording all moves and player's positions using moves. We are also checking game state in moves class (win/draw)
public class Moves {
    private final int rows = 6;
    private final int columns = 7;
    // Instance variables
    private String[][] board = new String[rows][columns];

    // Constructor to initialize
    public Moves() {
        for(int r = 0; r<rows; r++){
            Arrays.fill(board[r], null);
        }
    }

    public boolean makeMove(String playerID, int col){
        if(col<0 || col >= columns){
            return false;
        }
        for(int r = rows-1; r>=0; r--){
            if (board[r][col] == null){
                board[r][col] = playerID;
                return true;
            }
        }
        return false;
    }

    public boolean checkWin(String playerID){
        return checkVertical(playerID) || checkHorizontal(playerID) || checkDiagonal(playerID);
    }

    public boolean checkVertical(String playerID){
        for(int col = 0; col<columns; col++){
            int count = 0;
            for (int row = 0; row<rows; row++){
                if (playerID.equals(board[row][col])){
                    count++;
                    if(count == 4){return true;}
                } else {
                    count = 0;
                }
            }
        }
        return false;
    }

    public boolean checkHorizontal(String playerID){
        for(int row = 0; row<rows; row++){
            int count = 0;
            for(int col = 0; col<columns; col++){
                if(playerID.equals(board[row][col])){
                    count++;
                    if(count==4){return true;}
                } else {
                    count=0;
                }
            }
        }
        return false;
    }

    public boolean checkDiagonal(String playerID){
        for(int row = 0; row<rows-3; row++){
            for(int col = 0; col<columns-3; col++) {
                if(playerID.equals(board[row][col]) && playerID.equals(board[row+1][col+1])
                && playerID.equals(board[row+2][col+2]) && playerID.equals(board[row+3][col+3])
                ){return true;}
            }
        }
        for (int row = 3; row < rows; row++) {
            for (int col = 0; col < columns - 3; col++) {
                if (playerID.equals(board[row][col]) &&
                        playerID.equals(board[row - 1][col + 1]) &&
                        playerID.equals(board[row - 2][col + 2]) &&
                        playerID.equals(board[row - 3][col + 3])) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isBoardFull(){
        for(int row = 0; row<rows; row++){
            for(int col = 0; col<columns; col++){
                if(board[row][col] == null){
                    return false;
                }
            }
        }
        return true;
    }


    //Got this display from chatGPT
    public String getBoardString() {
        return Arrays.stream(board)
                .map(row -> Arrays.stream(row)
                        .map(cell -> cell == null ? "." : cell.substring(0, 1)) // Optional: abbreviate names
                        .collect(Collectors.joining(" ")))
                .collect(Collectors.joining("\n"));
    }

}
