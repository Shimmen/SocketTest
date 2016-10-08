package com.company;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientThread extends Thread {

    // Data types

    public interface OnDataListener {
        /**
         * Callback/listener for data from the server.
         *
         * This will be called from another thread so use a Handler (https://developer.android.com/reference/android/os/Handler.html)
         * if you need to interact with the main thread, i.e. UI related stuff.
         *
         * @param data the incoming data as key-value pairs
         */
        void onData(Map<String, String> data);
    }

    public enum State {
        WAIT_FOR_AND_SEND_OUTGOING_DATA,
        WAIT_FOR_INCOMING_DATA
    }

    // Data

    private final InetAddress serverAddress;

    private final Object socketLock = new Object();
    private volatile boolean socketShouldClose = false;

    private State currentState;
    private OnDataListener onDataListener;
    private Queue<Map<String, String>> sendQueue = new ConcurrentLinkedQueue<>();

    public ClientThread(String name, InetAddress serverAddress) {
        super(name);
        this.serverAddress= serverAddress;

        // The first thing a client does is send the 'connection packet'
        currentState = State.WAIT_FOR_AND_SEND_OUTGOING_DATA;
    }

    public void setOnDataListener(OnDataListener onDataListener) {
        this.onDataListener = onDataListener;
    }

    /**
     * Data submitted here will be sent to the server as soon as possible!
     *
     * @param data the data as key-value pairs
     */
    public void addDataToSendQueue(Map<String, String> data) {
        if (data != null) {
            synchronized (socketLock) {
                sendQueue.add(data);

                // Make sure the thread will send the newly added data
                currentState = State.WAIT_FOR_AND_SEND_OUTGOING_DATA;
            }
        }
/*
    FIXME: Implement this in a proper wait/notify way, without creating any deadlocks!
        synchronized (socketLock) {
            sendQueue.add(data);
            socketLock.notifyAll();
        }
*/
    }

    public void closeSocket() {
        synchronized (socketLock) {
            socketShouldClose = true;
        }
    }

    private boolean socketShouldClose() {
        synchronized (socketLock) {
            return socketShouldClose;
        }
    }

    @Override
    public void run() {
        try {

            System.out.println(getName() + ": starting up!");

            Socket socket = new Socket();
            socket.setReuseAddress(true);
            socket.connect(new InetSocketAddress(serverAddress, GameServer.PORT));

            if (!socket.isConnected()) {
                System.out.println(getName() + ": could not connect to the server! Closing down client.");
                return;
            } else {
                System.out.println(getName() + ": is now connected to server!");
            }

            while (!socketShouldClose()) {
                switch (currentState) {

                    case WAIT_FOR_INCOMING_DATA: {

                        Map<String, String> data = NetworkUtil.waitForAndReadData(socket);
                        onDataListener.onData(data);

                        // Set to the outgoing data stage.
                        // NOTE: This only works because the app always alternates between sending and receiving.
                        // It will never send twice or more in a row or receive twice or more in a row.
                        currentState = State.WAIT_FOR_AND_SEND_OUTGOING_DATA;

                    } break;

                    case WAIT_FOR_AND_SEND_OUTGOING_DATA: {

                        if (sendQueue.isEmpty()) {
                            // FIXME: Implement this in a proper wait/notify way, without creating any deadlocks!

                            // Just sleep for a while...
                            Thread.sleep(200);

                            // Wait until there is data in the send queue! We will be notified when there is data
                            //socketLock.wait();
                        } else {

                            // Send any data in the send queue
                            while (!sendQueue.isEmpty()) {
                                Map<String, String> data = sendQueue.poll();
                                NetworkUtil.sendData(data, socket);
                            }

                        }

                        // Set to the incoming data stage.
                        // NOTE: This only works because the app always alternates between sending and receiving.
                        // It will never send twice or more in a row or receive twice or more in a row.
                        currentState = State.WAIT_FOR_INCOMING_DATA;

                    } break;

                }
            }

            socket.close();

        } catch (IOException | InterruptedException e) {
            // TODO: Handle these a bit more gracefully!
            e.printStackTrace();
        }
    }

}
