package com.connect4.app.classes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
        for(int r = 0; r<rows; r++){
            if (board[r][col].isEmpty()){
                board[r][col] = playerID;
                return true;
            }
        }
        return false;
    }

}
