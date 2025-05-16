package com.connect4.app.GameLogic;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class GameClient {
    private WebSocketClient client;

    public GameClient(String serverUri) throws Exception {
        client = new WebSocketClient(new URI(serverUri)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("Connected to the server.");
            }

            @Override
            public void onMessage(String message) {
                System.out.println("Received: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Connection closed.");
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        };
        client.connect();
    }

    public void sendMove(String move) {
        client.send("gamerequest:"+"MOVE " + move);
    }

    public void createGame(String gameName) {
        client.send("gamerequest"+"CREATE_GAME " + gameName);
    }
}
