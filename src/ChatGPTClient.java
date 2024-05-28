import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatGPTClient {
    private String APIKey;
    private final String https_url = "https://api.openai.com/v1/chat/completions";

public ChatGPTClient() {
    System.out.println("Initializing ChatGPT Client");
    BufferedReader bufferedReader = null;
    try {
        FileReader fileReader = new FileReader(new File("API_Key.txt"));
        bufferedReader = new BufferedReader(fileReader);
        APIKey = bufferedReader.readLine();
        System.out.println("API Key loaded");
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


    public String queryChatGPT() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .headers("Authorization", "Bearer " + APIKey, "Content-Type", "application/json", "Accept", "application/json")
                .uri(URI.create(https_url))
                .POST(HttpRequest.BodyPublishers.ofString("{\"model\": \"gpt-3.5-turbo\",\"messages\": [{\"role\": \"user\", \"content\":\"Say this is a test\"}]}"))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Error querying ChatGPT";
    }
}
