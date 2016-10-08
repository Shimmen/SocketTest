package com.company;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class NetworkSimulation {

    public static void main(String[] args) {
        try {
            new NetworkSimulation().simulate();
        } catch (InterruptedException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void simulate() throws InterruptedException, UnknownHostException {

        //
        // Pre-multiplayer stuff
        //

        List<String> wordQuestions = new ArrayList<>();
        wordQuestions.add("Häst");
        wordQuestions.add("Apa");
        wordQuestions.add("Orangutang");
        wordQuestions.add("Myra");
        wordQuestions.add("Giraff");

        //
        // The WifiP2P part
        //
        InetAddress serverAddress = InetAddress.getLocalHost();
        //boolean isServer = true;

        //
        // The rest...
        //

        createServer(serverAddress, wordQuestions);

        // Make sure server is set up before clients start connecting to it!
        Thread.sleep(500);

        createClient(serverAddress, "Local client");

        // Make sure "Local client" becomes the local client by waiting some extra until connecting with the second one...
        // This is not ideal, but it works okay. If this is mixed up anyways it will simply confuse who is who and will
        // make one client's score the others.
        Thread.sleep(100);
        createClient(serverAddress, "Other client");
    }

    private void createServer(InetAddress serverAddress, List<String> wordQuestions) {
        new GameServer(serverAddress, wordQuestions).start();
    }

    private ClientThread createClient(InetAddress serverAddress, String clientName) {
        ClientThread client = new ClientThread(clientName, serverAddress);
        client.setOnDataListener((data) -> {
            clientOnData(client, data);
        });
        client.start();

        System.out.println(clientName + ": enqueueing ready packet to send queue!");
        Map<String, String> readyData = new HashMap<>();
        readyData.put("METHOD", "READY");
        client.addDataToSendQueue(readyData);

        return client;
    }

    private void clientOnData(ClientThread client, Map<String, String> data) {
        String clientName = client.getName();

        try {
            switch (data.get("METHOD")) {


                case "NEW_QUESTION":
                    System.out.println(clientName + ": received new question: " + data);

                    System.out.println(clientName + ": answering question...");
                    Thread.sleep(1000);
                    int currentQuestionResult = 10 + new Random().nextInt(10);

                    System.out.println(clientName + ": sending question results (" + currentQuestionResult + ")!");
                    Map<String, String> resultData = new HashMap<>();
                    resultData.put("QUESTION_RESULT", String.valueOf(currentQuestionResult));
                    client.addDataToSendQueue(resultData);
                    break;

                case "GAME_RESULTS":
                    System.out.println(clientName + ": received game results: " + data);
                    break;

                default:
                    System.err.println(clientName + "Client: got some unknown packet?" + data);

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
