package com.example.salarymanager.config;

import com.example.salarymanager.service.TelegramPollingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramPollingService telegramPollingService) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(telegramPollingService);
            System.out.println("🤖 텔레그램 봇(TelegramPollingService)이 성공적으로 등록되었습니다!");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return botsApi;
    }
}