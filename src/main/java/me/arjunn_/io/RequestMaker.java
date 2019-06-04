package me.arjunn_.io;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RequestMaker extends Thread {

    BufferedReader in;
    Client client;

    public RequestMaker(Client client) {
        in = new BufferedReader(new InputStreamReader(System.in));
        this.client = client;
    }

    @Override
    public void run() {

        while (true) {
            try {
                String send = in.readLine();
                if (send == null) {
                    continue;
                }
                JSONObject request = new JSONObject();
                request.put("event", "echo");
                request.put("data", send);

                try {
                    long start = System.currentTimeMillis();
                    JSONObject response = client.sendRequest(request).getResponse();
                    System.out.println(response);
                    System.out.println("Response time: " + (System.currentTimeMillis() - start) + " milliseconds.");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
