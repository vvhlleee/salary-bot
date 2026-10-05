package com.example.salarymanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramPollingService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final TelegramBotService telegramBotService;
    private final RestTemplate restTemplate = new RestTemplate();

    private long lastUpdateId = 0; // 마지막으로 읽은 메시지의 ID (중복 처리 방지용)

    // 3초마다(3000ms) 텔레그램 서버에 새로운 메시지가 있는지 확인
    @Scheduled(fixedRate = 3000)
    public void checkNewMessages() {
        try {
            String url = UriComponentsBuilder.newInstance()
                    .scheme("https")
                    .host("api.telegram.org")
                    .path("/bot" + botToken + "/getUpdates")
                    .queryParam("offset", lastUpdateId + 1)
                    .queryParam("timeout", 1)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");

                if (results != null) {
                    for (Map<String, Object> update : results) {
                        long updateId = ((Number) update.get("update_id")).longValue();
                        lastUpdateId = updateId; // 처리한 메시지 ID 갱신

                        Map<String, Object> message = (Map<String, Object>) update.get("message");
                        if (message != null && message.containsKey("text")) {
                            String text = (String) message.get("text");

                            // 봇에게 온 메시지를 앞서 만든 처리 서비스로 넘김
                            telegramBotService.processIncomingMessage(text);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 네트워크 오류 등은 무시하고 다음 주기에 재시도
        }
    }
}