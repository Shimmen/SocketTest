package com.company;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameServer extends Thread {

    // Data types

    enum State {
        WAIT_FOR_CLIENT_DATA,
        SEND_NEXT_QUESTION,
        WAIT_FOR_QUESTION_RESULTS,
        SEND_GAME_RESULTS,
        END_OF_GAME
    }

    // Static

    public static final int PORT = 61992;

    // State

    private InetAddress hostAddress;

    private State currentState = State.WAIT_FOR_CLIENT_DATA;

    private List<String> wordQuestions;
    private int currentQuestionIndex = 0;


    public GameServer(InetAddress hostAddress, List<String> wordQuestions) {
        super("TeamTimServerThread");
        this.hostAddress = hostAddress;
        this.wordQuestions = wordQuestions;
    }

    @Override
    public void run() {
        try {

            System.out.println("Server: starting up!");

            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(hostAddress, PORT));

            System.out.println("Server: waiting for client connections on " + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort());

            // Connect to the two clients. Will block until satisfied
            Socket client1 = serverSocket.accept();
            Socket client2 = serverSocket.accept();

            int client1Score = 0;
            int client2Score = 0;

            assert client1.isConnected();
            assert client2.isConnected();
            System.out.println("Server: successfully connected to client 1 (" + client1.getInetAddress() + ":" + client1.getPort() + ")");
            System.out.println("Server: successfully connected to client 2 (" + client2.getInetAddress() + ":" + client2.getPort() + ")");

            runLoop:
            while (currentState != State.END_OF_GAME) {
                switch (currentState) {

                    case WAIT_FOR_CLIENT_DATA: {

                        System.out.println("Server: waiting for client data!");

                        Map<String, String> c1Data = NetworkUtil.waitForAndReadData(client1);
                        System.out.println("Server: got packet from client 1: " + c1Data);

                        Map<String, String> c2Data = NetworkUtil.waitForAndReadData(client2);
                        System.out.println("Server: got packet from client 2: " + c2Data);

                        currentState = State.SEND_NEXT_QUESTION;
                    }
                    break;

                    case SEND_NEXT_QUESTION: {

                        assert currentQuestionIndex < wordQuestions.size();
                        System.out.println("Server: sending question!");

                        String question = wordQuestions.get(currentQuestionIndex);
                        Map<String, String> newQuestionData = new HashMap<>();
                        newQuestionData.put("METHOD", "NEW_QUESTION");
                        newQuestionData.put("QUESTION", question);
                        newQuestionData.put("C1SCORE", String.valueOf(client1Score));
                        newQuestionData.put("C2SCORE", String.valueOf(client2Score));

                        NetworkUtil.sendData(newQuestionData, client1);
                        NetworkUtil.sendData(newQuestionData, client2);

                        currentState = State.WAIT_FOR_QUESTION_RESULTS;
                    }
                    break;

                    case WAIT_FOR_QUESTION_RESULTS: {

                        System.out.println("Server: waiting for current question results!");

                        Map<String, String> c1ResultData  = NetworkUtil.waitForAndReadData(client1);
                        System.out.println("Server: got question result from client 1: " + c1ResultData);
                        client1Score += Integer.parseInt(c1ResultData.get("QUESTION_RESULT"));

                        Map<String, String> c2ResultData  = NetworkUtil.waitForAndReadData(client2);
                        System.out.println("Server: got question result from client 2: " + c2ResultData);
                        client2Score += Integer.parseInt(c2ResultData.get("QUESTION_RESULT"));

                        // Next question or end of game
                        currentQuestionIndex += 1;
                        if (currentQuestionIndex < wordQuestions.size()) {
                            currentState = State.SEND_NEXT_QUESTION;
                        } else {
                            System.out.println("Server: no more questions!");
                            currentState = State.SEND_GAME_RESULTS;
                        }
                    }
                    break;

                    case SEND_GAME_RESULTS: {
                        System.out.println("Server: sending total game results!");

                        Map<String, String> gameResultData = new HashMap<>();
                        gameResultData.put("METHOD", "GAME_RESULTS");
                        gameResultData.put("C1SCORE", String.valueOf(client1Score));
                        gameResultData.put("C2SCORE", String.valueOf(client2Score));

                        NetworkUtil.sendData(gameResultData, client1);
                        NetworkUtil.sendData(gameResultData, client2);

                        currentState = State.END_OF_GAME;
                    }
                    break;

                    case END_OF_GAME:
                    default: {
                        break runLoop;
                    }
                }
            }

            System.out.println("Server: end of game, shutting down!");

        } catch (IOException e) {
            // TODO: Handle somehow!
            e.printStackTrace();
        }
    }

}
