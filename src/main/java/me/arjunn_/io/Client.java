package me.arjunn_.io;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Client {

    public static Client client;

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static ArrayList<JSONObject> queue = new ArrayList<>();
    public static HashMap<String, Request<JSONObject>> futuresToResolve = new HashMap<>();

    private static String username;

    public Client() {

        // Connection and IO
        // Try reconnect every 5 seconds
        System.out.println("Establishing connection to server");

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    socket = new Socket("localhost", 9093);
                    this.cancel();

                    System.out.println("Connected to bridge at " + socket.getInetAddress());
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Connect and Authenticate
                    JSONObject auth = new JSONObject();
                    auth.put("event", "connect");
                    auth.put("name", username);
                    auth.put("auth", "abc123");
                    sendRequest(auth);

                    // Clear queued items
                    for (JSONObject send : queue) {
                        sendData(send);
                    }
                    queue.clear();


                    // Loop listening
                    while (true) {

                        try {
                            String s = in.readLine();
                            JSONObject received = new JSONObject(s);
                            handle(received);
                        } catch (IOException e) {
                            System.out.println("Disconnected.");
                            disconnect();
                            return;
                        }

                    }

                } catch (IOException e) {
                    System.out.println("Server offline! Attempting reconnection in 5 seconds.");
                }
            }
        }, 0L, 10 * 500L);


    }

    public static void main(String[] args) {
        System.out.print("Username: ");
        try {
            BufferedReader usernameGetter = new BufferedReader(new InputStreamReader(System.in));
            username = usernameGetter.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        client = new Client();
        new RequestMaker(client).start();
    }


    public Request<JSONObject> sendRequest(JSONObject request) {

        Request<JSONObject> response = new Request<>();
        request.put("type", "request");
        request.put("responseID", UUID.randomUUID().toString());
        Client.futuresToResolve.put(request.getString("responseID"), response);

        sendData(request);

        return response;

    }

    public void sendResponse(JSONObject received, JSONObject response) {

        response.put("type", "response");
        response.put("responseID", received.getString("responseID"));

        sendData(response);

    }


    public void sendData(JSONObject object) {
        if (socket != null && socket.isConnected()) {
            this.out.println(object.toString());
        } else {
            queue.add(object);
        }
    }

    public void disconnect() {
        try {
            socket.close();
            client = new Client();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void handle(JSONObject received) {

        if (!received.has("type")) {
            System.out.println("Received data with no type - ignoring. " + received);
            return;
        }

        String type = received.getString("type");

        if (type.equals("response")) {
            if (!received.has("responseID")) {
                System.out.println("Received response without a responseID - ignoring. " + received);
            } else if (futuresToResolve.containsKey(received.getString("responseID"))) {
                futuresToResolve.get(received.getString("responseID")).setResponse(received);
                futuresToResolve.remove(received.getString("responseID"));
            } else {
                System.out.println("Got a response for a non-existant request! Request either never existed or has timed out." + received );
            }
            return;
        }

        if (!received.has("event")) {
            System.out.println("Received a request without an event: " + received);
            return;
        }

        // Received a valid request
        String event = received.getString("event");

        // Begin handling based on event name

        if (event.equalsIgnoreCase("echo" )) {
            JSONObject response = new JSONObject();
            response.put("event", "echo");
            response.put("data", received.getString("data"));
            client.sendResponse(received, response);
        }

    }

}
