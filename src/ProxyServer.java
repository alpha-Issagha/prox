import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyServer {
    private static final int PORT = 8080;
    private static final int THREAD_POOL_SIZE = 10;
    private static final String CAS_URL = "https://identites.ensea.fr/cas/v1/tickets";
    private static final String SERVICE_VALIDATE_URL = "https://identites.ensea.fr/cas/serviceValidate";
    private static final String SERVICE_URL = "http://localhost"; // Utilisé pour la validation du ticket

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        server.createContext("/", new ProxyHandler(threadPool));
        server.setExecutor(null);
        System.out.println("Proxy server listening on port " + PORT);
        server.start();
    }

    static class ProxyHandler implements HttpHandler {
        private final ExecutorService threadPool;

        public ProxyHandler(ExecutorService threadPool) {
            this.threadPool = threadPool;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String username = exchange.getRequestHeaders().getFirst("Username");
                String password = exchange.getRequestHeaders().getFirst("Password");

                if (username == null || password == null) {
                    sendResponse(exchange, "Missing Username or Password in Headers", 400);
                    return;
                }

                threadPool.submit(() -> {
                    try {
                        String tgt = getTicketGrantingTicket(username, password);
                        System.out.println("TGT: " + tgt);
                        if (tgt != null) {
                            String st = getServiceTicket(tgt, SERVICE_URL);
                            System.out.println("ST: " + st);
                            if (st != null && validateServiceTicket(st, SERVICE_URL)) {
                                ChatGPTClient chatGPTClient = new ChatGPTClient();
                                String response = chatGPTClient.queryChatGPT();
                                sendResponse(exchange, response, 200);
                            } else {
                                sendResponse(exchange, "CAS Authentication Failed 1", 401);
                            }
                        } else {
                            sendResponse(exchange, "CAS Authentication Failed 2", 401);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            sendResponse(exchange, "Internal Server Error", 500);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            } else {
                sendResponse(exchange, "Method Not Allowed", 405);
            }
        }

        private String getTicketGrantingTicket(String username, String password) {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CAS_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("username=" + username + "&password=" + password))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("TGT Response Code: " + response.statusCode());
                System.out.println("TGT Response Body: " + response.body());
                if (response.statusCode() == 201) {
                    String location = response.headers().firstValue("Location").orElse(null);
                    if (location != null) {
                        return location.substring(location.lastIndexOf("/") + 1);
                    }
                } else {
                    System.out.println("Failed to obtain TGT: " + response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private String getServiceTicket(String tgt, String serviceUrl) {
            HttpClient client = HttpClient.newHttpClient();
            try {
                String encodedServiceUrl = URLEncoder.encode(serviceUrl, StandardCharsets.UTF_8.toString());
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(CAS_URL + "/" + tgt))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("service=" + encodedServiceUrl))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("ST Request URL: " + CAS_URL + "/" + tgt);
                System.out.println("ST Request Body: " + "service=" + encodedServiceUrl);
                System.out.println("ST Response Code: " + response.statusCode());
                System.out.println("ST Response Body: " + response.body());
                if (response.statusCode() == 200) {
                    return response.body();
                } else {
                    System.out.println("Failed to obtain ST: " + response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private boolean validateServiceTicket(String serviceTicket, String serviceUrl) {
            HttpClient client = HttpClient.newHttpClient();
            try {
                String encodedServiceUrl = URLEncoder.encode(serviceUrl, StandardCharsets.UTF_8.toString());
                String validationUrl = SERVICE_VALIDATE_URL + "?ticket=" + serviceTicket + "&service=" + encodedServiceUrl;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(validationUrl))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Validation Request URL: " + validationUrl);
                System.out.println("Validation Response Code: " + response.statusCode());
                System.out.println("Validation Response Body: " + response.body());
                if (response.statusCode() == 200) {
                    return response.body().contains("authenticationSuccess");
                } else {
                    System.out.println("Failed to validate ST: " + response.body());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}
