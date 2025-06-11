package com.example.chatbot_webview;

import com.example.chatbot_webview.MyBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "https://chatbot-webview.vercel.app")
@RestController
@RequestMapping("/api/telegram")
public class TelegramController {

    @Autowired
    private MyBot myBot;

    @PostMapping("/send")
    public String sendMessageToTelegram(@RequestBody TelegramRequest request) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(request.getChatId());
        sendMessage.setText(request.getMessage());

        try {
            myBot.execute(sendMessage);
            return "Message sent!";
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return "Failed to send message";
        }
    }

    public static class TelegramRequest {
        private String chatId;
        private String message;

        public String getChatId() {
            return chatId;
        }

        public void setChatId(String chatId) {
            this.chatId = chatId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}
