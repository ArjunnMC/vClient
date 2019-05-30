
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class Client {

    public static Client client;

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static ArrayList<JSONObject> queue = new ArrayList<>();
    private static HashMap<String, CompletableFuture<String>> futuresToResolve = new HashMap<>();

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
                    auth.put("EVENT", "CONNECT");
                    auth.put("name", "vCore-CLIENT");
                    auth.put("auth", "abc123");
                    auth.put("responseID", "000");
                    sendData(auth);

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
        client = new Client();
        new RequestMaker(client);
    }

    public CompletableFuture<String> getStringFromServer(JSONObject request) {
        CompletableFuture<String> response = new CompletableFuture<>();
        futuresToResolve.put(request.getString("responseID"), response);

        sendData(request);

        return response;

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

    public static void handle(JSONObject sent) {

        System.out.println("Received data: " + sent);

        if (sent.has("responseID")) {
            if (futuresToResolve.containsKey(sent.getString("responseID"))) {
                futuresToResolve.get(sent.getString("responseID")).complete(sent.getString("DATA"));
            }
        }

    }


//    public static ClientResponseHandler sendDataGetResponse(JSONObject request) {
//
//        client.sendData(request);
//        ClientResponseHandler handler = new ClientResponseHandler(request.getString("responseID"), 40L);
//        handlers.add(handler);
//        handler.start();
//        return handler;
//
//    }


}
