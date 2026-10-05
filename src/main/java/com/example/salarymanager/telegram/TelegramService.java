package com.example.salarymanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    // 텔레그램으로 메시지를 보내는 메서드 (POST JSON 방식)
    public void sendMessage(String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        RestTemplate restTemplate = new RestTemplate();

        // JSON 형식으로 데이터 세팅
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", message);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}