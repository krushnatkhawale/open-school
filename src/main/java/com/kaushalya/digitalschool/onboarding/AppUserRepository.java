package com.kaushalya.digitalschool.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByTelegramChatId(Long telegramChatId);

    @Query("SELECT u.telegramChatId FROM AppUser u WHERE u.school.id = :schoolId")
    List<Long> findTelegramChatIdsBySchoolId(@Param("schoolId") UUID schoolId);
}
