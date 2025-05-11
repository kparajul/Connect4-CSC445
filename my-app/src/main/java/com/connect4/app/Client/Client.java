package com.connect4.app.Client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class Client extends WebSocketClient {

    public Client(URI server) {
        super(server);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        System.out.println("Connected to server");
    }

    @Override
    public void onMessage(String s) {
        System.out.println("Server: " + s);
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        System.out.println("Disconnected because: " + s);
    }

    @Override
    public void onError(Exception e) {
        System.out.println("Error: " + e.getMessage());
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        URI server = new URI("ws://localhost:3635");
        Client client = new Client(server);
        client.connectBlocking();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Type command:\n");
        while (true){
            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("exit")){
                client.close();
                break;
            }
            client.send(command);
        }
    }


}
