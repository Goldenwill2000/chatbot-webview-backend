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

    private static final String PHONE_ID = "689368164260551"; // your WhatsApp business phone ID

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

                if (message.containsKey("text")) {
                    String text = (String) ((Map<String, Object>) message.get("text")).get("body");
                    if ("hi".equalsIgnoreCase(text)) {
                        sendWelcomeOptions(from);
                    }
                } else if (message.containsKey("button")) {
                    String buttonId = (String) ((Map<String, Object>) message.get("button")).get("payload");

                    switch (buttonId) {
                        case "proposed_order":
                            sendProposedOrderOptions(from);
                            break;
                        case "order_history":
                            sendMessage(from, "Showing your order history...");
                            break;
                        case "leave":
                            sendMessage(from, "You chose to leave.");
                            break;
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

    private void sendWelcomeOptions(String to) throws IOException, InterruptedException {
        Map<String, Object> message = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "interactive",
                "interactive", Map.of(
                        "type", "button",
                        "body", Map.of("text", "Hi! Select one of the options:"),
                        "action", Map.of(
                                "buttons", List.of(
                                        Map.of("type", "reply", "reply", Map.of("id", "proposed_order", "title", "Proposed Order")),
                                        Map.of("type", "reply", "reply", Map.of("id", "order_history", "title", "Order History"))
                                )
                        )
                )
        );
        sendWhatsAppRequest(message);
    }

    private void sendProposedOrderOptions(String to) throws IOException, InterruptedException {
        String webviewUrl = "https://chatbot-webview.vercel.app?phone=" + to;

        Map<String, Object> message = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "interactive",
                "interactive", Map.of(
                        "type", "button",
                        "body", Map.of("text", "Click below to view more information."),
                        "action", Map.of(
                                "buttons", List.of(
                                        Map.of("type", "url", "url", webviewUrl, "title", "More Info"),
                                        Map.of("type", "reply", "reply", Map.of("id", "leave", "title", "Leave"))
                                )
                        )
                )
        );
        sendWhatsAppRequest(message);
    }

    private void sendMessage(String to, String text) throws IOException, InterruptedException {
        Map<String, Object> message = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", text)
        );
        sendWhatsAppRequest(message);
    }

    private void sendWhatsAppRequest(Map<String, Object> payload) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/v18.0/" + PHONE_ID + "/messages"))
                .header("Authorization", "Bearer " + whatsappToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("WhatsApp response: " + response.statusCode() + " - " + response.body());
    }
}