package com.example.chatbot_webview;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;

@Component
public class MyBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "JavaT3stBot";
    }

    @Override
    public String getBotToken() {
        return "7826648391:AAEUENATbIVDngbwsu_wZOisUbuPhhqvHzk";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userMessage = update.getMessage().getText().trim();
            Long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getFrom().getFirstName();

            if (userMessage.equalsIgnoreCase("hi")) {
                sendWelcomeMessage(chatId, firstName);
            }

        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String firstName = update.getCallbackQuery().getFrom().getFirstName();

            if (data.equals("Proposed Order")) {
                proposedOrderOptionReply(firstName ,chatId);
            } else if (data.equals("Order History")) {
                sendTextMessage(chatId, "Showing your order history...");
            } else if (data.equals("Leave")) {
                sendTextMessage(chatId, "You chose to leave.");
            }
        }
    }

    private void proposedOrderOptionReply(String firstName, Long chatId) {
        String text = "What would you like to do within Proposed Order?";

        InlineKeyboardButton webAppButton = new InlineKeyboardButton();
        webAppButton.setText("More Info");

        String baseUrl = "https://rss-test.np.accenture.com/CBLA_Lite_Web/bo-redirect?param=eyJ1c2VybmFtZSI6Ik5QVEhPIiwicGFzc3dvcmQiOiI5Y2MzZTVhYmRmYWVlMTk4YTZiNzRiOTdlYTkxZTMwNmUxNzQ0ZGRmNDk4NzZkM2RjZjZjYjkzNmRmZDVjOWQ3IiwicG9zSWQiOiJCTEJDIiwiZW52IjoiU0lNIiwicmVkaXJlY3QiOiJTQUxFU19PUkRFUiJ9";
        String webAppUrl = baseUrl + "&chatId=" + chatId + "&firstName=" + URLEncoder.encode(firstName, StandardCharsets.UTF_8);
        System.out.println(webAppUrl+"test");
        webAppButton.setUrl(webAppUrl);

        InlineKeyboardButton leaveButton = new InlineKeyboardButton();
        leaveButton.setText("Leave");
        leaveButton.setCallbackData("Leave");

        List<InlineKeyboardButton> row = List.of(webAppButton, leaveButton);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(row));

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendWelcomeMessage(Long chatId, String firstName) {
        String text = "Hi, " + firstName + "!\nSelect one of the options:";

        InlineKeyboardButton proposedOrder = new InlineKeyboardButton();
        proposedOrder.setText("Proposed Order");
        proposedOrder.setCallbackData("Proposed Order");

        InlineKeyboardButton orderHistory = new InlineKeyboardButton();
        orderHistory.setText("Order History");
        orderHistory.setCallbackData("Order History");

        List<InlineKeyboardButton> row = List.of(proposedOrder, orderHistory);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup(List.of(row));

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
