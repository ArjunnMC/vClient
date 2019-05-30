import org.json.JSONObject;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RequestMaker extends Thread {

    private Client client;

    public RequestMaker(Client client) {
        this.client = client;
        JSONObject request = new JSONObject();
        request.put("EVENT", "ECHO");
        request.put("MESSAGE", "Hello there");
        request.put("responseID", UUID.randomUUID().toString());
        System.out.println(request.getString("responseID"));

        try {
            CompletableFuture<String> future = client.getStringFromServer(request);
            String response = future.get(100, TimeUnit.MILLISECONDS);
            System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
