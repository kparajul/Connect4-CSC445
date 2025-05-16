package com.connect4.app.Client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

public class Client extends WebSocketClient {
    private static URI serverURI;
    private static boolean exitProper;

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
        new Thread(() -> {
            while (!isOpen() && !exitProper){
                try {
                    System.out.println("Reconnecting");
                    Client nClient = new Client(serverURI);
                    nClient.connectBlocking();
                    nClient.runCommandLoop();

                    break;
                }catch (Exception e){
                    System.out.println("Reconnection failed");
                    try{
                        Thread.sleep(100);
                    } catch (InterruptedException ignored){}
                }
            }
        }).start();
    }

    public void runCommandLoop(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Type command:\n");
        while (true){
            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("exit")){
                exitProper = true;
                this.close();
                break;
            }
            this.send(command);
        }
    }

    @Override
    public void onError(Exception e) {
        System.out.println("Error: " + e.getMessage());
    }

    public static void main(String[] args) throws URISyntaxException, InterruptedException {
        serverURI = new URI("ws://localhost:26960");
        Client client = new Client(serverURI);
        client.connectBlocking();
        client.runCommandLoop();
    }


}
