package com.example.salarymanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final TelegramPollingService telegramPollingService;

    @Value("${telegram.bot.chat-id}")
    private String defaultChatId;

    // chatId 없이 메시지만 들어오면 기본 chat-id로 전송
    public void sendMessage(String message) {
        try {
            if (defaultChatId != null && !defaultChatId.isEmpty()) {
                long chatId = Long.parseLong(defaultChatId);
                telegramPollingService.sendMessage(chatId, message);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // [중요] 특정 사용자의 chatId로 명시적으로 메시지 전송 (Long 타입)
    public void sendMessage(Long chatId, String message) {
        if (chatId != null) {
            telegramPollingService.sendMessage(chatId, message);
        }
    }

    // 오버로딩: String 타입의 chatId가 들어올 경우 처리
    public void sendMessage(String targetChatId, String message) {
        try {
            if (targetChatId != null && !targetChatId.isEmpty()) {
                long chatId = Long.parseLong(targetChatId);
                telegramPollingService.sendMessage(chatId, message);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}