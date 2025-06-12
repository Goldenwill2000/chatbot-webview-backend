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

    private final String WEB_APP_BASE_URL = "https://chatbot-webview.vercel.app";

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> entry = ((List<Map<String, Object>>) payload.get("entry")).get(0);
            Map<String, Object> changes = ((List<Map<String, Object>>) entry.get("changes")).get(0);
            Map<String, Object> value = (Map<String, Object>) changes.get("value");

            List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

            if (messages != null && !messages.isEmpty()) {
                Map<String, Object> message = messages.get(0);
                String from = (String) message.get("from");

                if (message.containsKey("text")) {
                    String text = ((Map<String, Object>) message.get("text")).get("body").toString().trim().toLowerCase();

                    switch (text) {
                        case "hi":
                            sendInitialOptions(from);
                            break;
                        case "order history":
                            sendSimpleMessage(from, "Here is your order history...");
                            break;
                        case "proposed order":
                            sendProposedOrderOptions(from);
                            break;
                        case "leave":
                            sendSimpleMessage(from, "You chose to leave.");
                            break;
                        default:
                            sendSimpleMessage(from, "Please choose an option: hi, order history, or proposed order.");
                            break;
                    }
                } else if (message.containsKey("button")) {
                    Map<String, Object> button = (Map<String, Object>) message.get("button");
                    String buttonText = button.get("text").toString().toLowerCase();

                    if ("proposed order".equals(buttonText)) {
                        sendProposedOrderOptions(from);
                    } else if ("order history".equals(buttonText)) {
                        sendSimpleMessage(from, "Here is your order history...");
                    } else if ("leave".equals(buttonText)) {
                        sendSimpleMessage(from, "You chose to leave.");
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
        String webviewUrl = WEB_APP_BASE_URL + "?phone=" + to;

        Map<String, Object> message = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "interactive",
                "interactive", Map.of(
                        "type", "button",
                        "body", Map.of("text", "What would you like to do within Proposed Order?"),
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

    private void sendSimpleMessage(String to, String bodyText) throws IOException, InterruptedException {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", to,
                "type", "text",
                "text", Map.of("body", bodyText)
        );
        sendWhatsAppRequest(payload);
    }

    private void sendWhatsAppRequest(Map<String, Object> message) throws IOException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(message);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/v19.0/689368164260551/messages"))
                .header("Authorization", "Bearer " + whatsappToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("WhatsApp API response: " + response.statusCode() + " - " + response.body());
    }
}
