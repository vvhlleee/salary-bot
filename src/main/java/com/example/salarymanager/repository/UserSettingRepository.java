package com.example.salarymanager.repository;

import com.example.salarymanager.domain.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
    Optional<UserSetting> findByUserId(Long userId);
}