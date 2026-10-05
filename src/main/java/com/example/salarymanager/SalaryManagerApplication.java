package com.example.salarymanager;

import com.example.salarymanager.service.TelegramService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling; // <-- 추가

@SpringBootApplication
@EnableScheduling // <-- 이 줄을 꼭 추가해주세요!
public class SalaryManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalaryManagerApplication.class, args);
    }

    @Bean
    CommandLineRunner init(TelegramService telegramService) {
        return args -> {
            telegramService.sendMessage("🚀 [Salary Manager] 서버가 성공적으로 켜졌습니다! 텔레그램 연동 완료!");
        };
    }
}