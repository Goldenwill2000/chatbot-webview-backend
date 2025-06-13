package com.example.chatbot_webview;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@RestController
@RequestMapping("/webhook")
public class WhatsAppWebhookController {

    @Value("${TOKEN}")
    private String whatsappToken;

    @Value("${MYTOKEN}")
    private String verifyToken;

    private final String WHATSAPP_API_URL = "https://graph.facebook.com/v19.0/689368164260551/messages";
    private final String WEBAPP_URL = "https://chatbot-webview.vercel.app";

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📨 Incoming webhook payload:\n" + new ObjectMapper().writeValueAsString(payload));

            var entry = ((List<Map<String, Object>>) payload.get("entry")).get(0);
            var change = ((List<Map<String, Object>>) entry.get("changes")).get(0);
            var value = (Map<String, Object>) change.get("value");
            var messages = (List<Map<String, Object>>) value.get("messages");

            if (messages != null && !messages.isEmpty()) {
                var message = messages.get(0);
                System.out.println("🧾 Extracted message: " + message);
                String from = (String) message.get("from");

                // Text message
                if (message.containsKey("text")) {
                    String text = ((Map<String, String>) message.get("text")).get("body");
                    if ("hi".equalsIgnoreCase(text.trim())) {
                        sendInitialOptions(from);
                    }
                }

                // Button click
                if (message.containsKey("button")) {
                    Map<String, Object> button = (Map<String, Object>) message.get("button");
                    String payloadId = (String) button.get("payload");
                    System.out.println("🎯 Button clicked with payload: " + payloadId);

                    switch (payloadId) {
                        case "proposed_order":
                            sendCTA(from);
                            break;
                        case "order_history":
                            sendSimpleMessage(from, "Here is your order history...");
                            break;
                        case "leave":
                            sendSimpleMessage(from, "You chose to leave.");
                            break;
                        default:
                            sendSimpleMessage(from, "Unknown option.");
                    }
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
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    private void sendInitialOptions(String to) throws IOException, InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");

        Map<String, String> body = Map.of("text", "Hi! Select an option:");
        interactive.put("body", body);

        List<Map<String, Object>> buttons = new ArrayList<>();
        buttons.add(button("proposed_order", "Proposed Order"));
        buttons.add(button("order_history", "Order History"));
        buttons.add(button("leave", "Leave"));

        interactive.put("action", Map.of("buttons", buttons));
        payload.put("interactive", interactive);

        sendToWhatsAppAPI(payload);
    }

    private void sendCTA(String to) throws IOException, InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        payload.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "cta_url");

        // Header
        Map<String, Object> header = new HashMap<>();
        header.put("type", "text");
        header.put("text", "Order Ready");
        interactive.put("header", header);

        // Body
        Map<String, Object> body = new HashMap<>();
        body.put("text", "Click below to view your order details.");
        interactive.put("body", body);

        // Action
        Map<String, Object> action = new HashMap<>();
        action.put("name", "cta_url");
        action.put("parameters", Map.of(
                "display_text", "View More",
                "url", WEBAPP_URL
        ));
        interactive.put("action", action);

        // Footer (optional)
        Map<String, Object> footer = new HashMap<>();
        footer.put("text", "Powered by ChatBot");
        interactive.put("footer", footer);

        payload.put("interactive", interactive);

        sendToWhatsAppAPI(payload);
    }

    private void sendSimpleMessage(String to, String text) throws IOException, InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "text");
        payload.put("text", Map.of("body", text));

        sendToWhatsAppAPI(payload);
    }

    private Map<String, Object> button(String id, String title) {
        return Map.of(
                "type", "reply",
                "reply", Map.of(
                        "id", id,
                        "title", title
                )
        );
    }

    private void sendToWhatsAppAPI(Map<String, Object> payload) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(payload);
        System.out.println("🚀 Sending payload to WhatsApp:\n" + json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WHATSAPP_API_URL))
                .header("Authorization", "Bearer " + whatsappToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("📬 WhatsApp API response: " + response.statusCode() + " - " + response.body());
    }
}
