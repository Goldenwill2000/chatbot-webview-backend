package com.example.chatbot_webview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> entry = ((List<Map<String, Object>>) payload.get("entry")).get(0);
            Map<String, Object> changes = ((List<Map<String, Object>>) entry.get("changes")).get(0);
            Map<String, Object> value = (Map<String, Object>) changes.get("value");

            List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

            if (messages != null) {
                Map<String, Object> message = messages.get(0);
                String from = (String) message.get("from");
                Map<String, Object> textObj = (Map<String, Object>) message.get("text");
                String text = (String) textObj.get("body");

                // respond based on message
                if ("hi".equalsIgnoreCase(text)) {
                    sendMessage(from, "Hello! How can I assist you?");
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {

        if ("subscribe".equals(mode) && "abc123".equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private void sendMessage(String to, String message) throws IOException {
        String url = "https://graph.facebook.com/v22.0/689368164260551/messages";

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "text");

        Map<String, String> text = new HashMap<>();
        text.put("body", message);
        payload.put("text", text);

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(payload);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "EAAUOGAq2S8wBO4nljvre2FoT7MiV9FI54v2QkWEAnaQ4ds9cEZBcZCMsBMSNVOW9Gf63YM8UnZB0RZAIWjj4QQCpshhqNwCcngDN1ftI4f55qkKuZAHZAFtZBl32RHRtR1BqEExdSnatxnbwRPowKbn2EUYMjrHGLLwAFTosQZC8mCoZCbEGJWil2ivFWDaMQJzaEAo7jHvtYw349vpMWBeKiFzQMWLGwuDIZD")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}