package com.example.chatbot_webview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class ChatbotWebviewApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatbotWebviewApplication.class, args);
	}

//	@Bean
//	public TelegramBotsApi telegramBotsApi(MyBot myBot) throws TelegramApiException {
//		TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
//		botsApi.registerBot(myBot);
//		return botsApi;
//	}

}