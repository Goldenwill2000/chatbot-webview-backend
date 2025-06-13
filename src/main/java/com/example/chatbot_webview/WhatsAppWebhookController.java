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

    private final String WHATSAPP_API_URL = "https://graph.facebook.com/v22.0/689368164260551/messages";
    private final String WEBAPP_URL = "https://rss-test.np.accenture.com/CBLA_Lite_Web/bo-redirect?param=eyJ1c2VybmFtZSI6Ik5QVEhPIiwicGFzc3dvcmQiOiI5Y2MzZTVhYmRmYWVlMTk4YTZiNzRiOTdlYTkxZTMwNmUxNzQ0ZGRmNDk4NzZkM2RjZjZjYjkzNmRmZDVjOWQ3IiwicG9zSWQiOiJCTEJDIiwiZW52IjoiU0lNIiwicmVkaXJlY3QiOiJTQUxFU19PUkRFUiJ9";

    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println("📥 Incoming webhook payload:\n" + payload);

            var entry = ((List<Map<String, Object>>) payload.get("entry")).get(0);
            var change = ((List<Map<String, Object>>) entry.get("changes")).get(0);
            var value = (Map<String, Object>) change.get("value");
            var messages = (List<Map<String, Object>>) value.get("messages");

            if (messages != null && !messages.isEmpty()) {
                var message = messages.get(0);
                System.out.println("🔍 Extracted message: " + message);

                String from = (String) message.get("from");

                if (message.containsKey("text")) {
                    String text = ((Map<String, String>) message.get("text")).get("body");
                    if ("hi".equalsIgnoreCase(text.trim())) {
                        sendInitialOptions(from);
                    }
                }

                if (message.containsKey("interactive")) {
                    Map<String, Object> interactive = (Map<String, Object>) message.get("interactive");
                    String interactiveType = (String) interactive.get("type");

                    if ("button_reply".equals(interactiveType)) {
                        Map<String, String> buttonReply = (Map<String, String>) interactive.get("button_reply");
                        String payloadId = buttonReply.get("id");
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
        String countryCode = extractCountryCode(to);

        String urlWithCC = WEBAPP_URL + "?cc=" + countryCode;

        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "text");
        payload.put("text", Map.of(
                "body", "Your order is ready! 🔗 Click here to view: " + urlWithCC
        ));

        sendToWhatsAppAPI(payload);
    }

    private String extractCountryCode(String phone) {
        if (phone.startsWith("1")) return "1";
        if (phone.startsWith("60")) return "60";     // Malaysia
        if (phone.startsWith("65")) return "65";     // Singapore
        if (phone.length() >= 3) return phone.substring(0, 3);
        if (phone.length() >= 2) return phone.substring(0, 2);
        return "unknown";
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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WHATSAPP_API_URL))
                .header("Authorization", "Bearer " + whatsappToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("📤 WhatsApp API response: " + response.statusCode() + " - " + response.body());
    }
}
