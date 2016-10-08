package com.company;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class NetworkUtil {

    private NetworkUtil() {

    }

    public static Map<String, String> waitForAndReadData(Socket socket) {
        try {

            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
            BufferedReader in = new BufferedReader(inputStreamReader);

            // Read packet as a line
            String line = in.readLine();

            // Convert raw data to a nice map
            return NetworkUtil.dataFromString(line);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void sendData(Map<String, String> data, Socket socket) {
        try {

            byte[] rawData = NetworkUtil.asByteArray(data);
            socket.getOutputStream().write(rawData);

            // Not optimal to write this every time but easier to handle
            socket.getOutputStream().flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> dataFromString(String packet) {
        Map<String, String> data = new HashMap<>();

        String[] keyValuePairs = packet.split(";");
        for (String pair: keyValuePairs) {
            String[] s = pair.split("=");
            data.put(s[0], s[1]);
        }

        return data;
    }

    private static byte[] asByteArray(Map<String, String> data) {
        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String stringEntry = entry.getKey() + "=" + entry.getValue() + ";";
            builder.append(stringEntry);
        }

        // Since we now use BufferedReader.readLine() it requires line breaks. This works well, though, since a line
        // break can signify a packet boundary.
        builder.append('\n');

        String stringData = builder.toString();
        return stringData.getBytes(Charset.forName("UTF-8"));
    }

}
