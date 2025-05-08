package com.connect4.app.GameLogic;

public class RaftManager {
    private static volatile boolean leader = false;

    public static boolean isLeader(){
        return leader;
    }

    public static void electLeader(boolean l){
        leader = l;
    }

    public static void chooseNewLeader(boolean n){
        leader = n;
    }
}
