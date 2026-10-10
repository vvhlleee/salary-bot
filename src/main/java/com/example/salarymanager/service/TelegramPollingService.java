package com.example.salarymanager.service;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelegramPollingService extends TelegramLongPollingBot {

    // 사용자별 잔액 저장 (실제 서비스에서는 DB나 Repository를 연동하여 사용하세요)
    private final Map<Long, Double> userBalances = new ConcurrentHashMap<>();

    // 사용자별 현재 대화 상태 관리 (WAITING_FOR_DEPOSIT, WAITING_FOR_EXPENSE, IDLE)
    private final Map<Long, String> userStates = new ConcurrentHashMap<>();

    @Override
    public String getBotUsername() {
        return "your_bot_username"; // 본인의 봇 유저네임
    }

    @Override
    public String getBotToken() {
        return "your_bot_token"; // 본인의 봇 토큰
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText().trim();

        // 현재 사용자의 대화 상태 가져오기 (기본값: IDLE)
        String currentState = userStates.getOrDefault(chatId, "IDLE");

        // 1. 명령어 처리 (/입금 또는 /지출)
        if (messageText.equals("/입금")) {
            userStates.put(chatId, "WAITING_FOR_DEPOSIT");
            sendMessage(chatId, "얼마를 입금하시겠습니까? 금액을 숫자로 입력해주세요.");
            return;
        } else if (messageText.equals("/지출")) {
            userStates.put(chatId, "WAITING_FOR_EXPENSE");
            sendMessage(chatId, "얼마를 지출하시겠습니까? 금액을 숫자로 입력해주세요.");
            return;
        }

        // 2. 상태에 따른 금액 입력 처리
        if (currentState.equals("WAITING_FOR_DEPOSIT")) {
            try {
                double amount = Double.parseDouble(messageText);
                double currentBalance = userBalances.getOrDefault(chatId, 0.0) + amount;
                userBalances.put(chatId, currentBalance);

                userStates.remove(chatId); // 상태 초기화
                sendMessage(chatId, String.format("입금이 완료되었습니다. 💰 현재 잔액: %.0f원", currentBalance));
            } catch (NumberFormatException e) {
                sendMessage(chatId, "올바른 숫자 형태만 입력해주세요. 다시 입력해 주세요.");
            }
        } else if (currentState.equals("WAITING_FOR_EXPENSE")) {
            try {
                double amount = Double.parseDouble(messageText);
                double currentBalance = userBalances.getOrDefault(chatId, 0.0) - amount;
                userBalances.put(chatId, currentBalance);

                userStates.remove(chatId); // 상태 초기화
                sendMessage(chatId, String.format("지출이 반영되었습니다. 💸 현재 잔액: %.0f원", currentBalance));
            } catch (NumberFormatException e) {
                sendMessage(chatId, "올바른 숫자 형태만 입력해주세요. 다시 입력해 주세요.");
            }
        } else {
            // 그 외 일반 텍스트 입력 시 안내
            sendMessage(chatId, "명령어를 선택해주세요.\n/입금 - 잔액 추가\n/지출 - 잔액 차감");
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}