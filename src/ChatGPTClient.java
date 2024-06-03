import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ChatGPTClient {
    private String APIKey;
    private final String https_url = "https://api.openai.com/v1/chat/completions";

    public ChatGPTClient() {
        try {
            FileReader fileReader = new FileReader(new File("API_Key.txt"));
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            APIKey = bufferedReader.readLine();
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String queryChatGPT(String question) {
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = String.format("{\"model\": \"gpt-3.5-turbo\",\"messages\": [{\"role\": \"user\", \"content\":\"%s\"}]}", question);

        HttpRequest request = HttpRequest.newBuilder()
                .headers("Authorization", "Bearer " + APIKey, "Content-Type", "application/json", "Accept", "application/json")
                .uri(URI.create(https_url))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: Unable to get response from ChatGPT";
        }
    }
}
