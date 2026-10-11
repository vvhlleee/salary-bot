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
    public TelegramBotsApi telegramBotsApi(TelegramPollingService telegramPollingService) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramPollingService);
            System.out.println("🤖 [성공] TelegramPollingService가 TelegramBotsApi에 정상 등록되었습니다!");
            return botsApi;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            throw new RuntimeException("텔레그램 봇 API 등록 중 오류 발생", e);
        }
    }
}