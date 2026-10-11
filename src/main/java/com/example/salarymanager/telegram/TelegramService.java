@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String defaultChatId;

    // 1. 기존 코드들이 그대로 쓰도록 둔 메서드 (서버 알림, 스케줄러용)
    public void sendMessage(String message) {
        sendMessage(defaultChatId, message);
    }

    // 2. 대화형 봇 답장용으로 새로 추가한 메서드 (특정 유저에게 전송)
    public void sendMessage(String targetChatId, String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", targetChatId);
        body.put("text", message);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}