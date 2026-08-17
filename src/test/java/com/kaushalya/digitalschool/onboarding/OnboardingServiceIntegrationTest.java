package com.kaushalya.digitalschool.onboarding;

import com.kaushalya.digitalschool.classification.AiChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:onboardingtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OnboardingServiceIntegrationTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            return Mockito.mock(AiChatService.class);
        }
    }

    @Autowired
    private OnboardingService onboardingService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    @BeforeEach
    void cleanDb() {
        appUserRepository.deleteAll();
        schoolRepository.deleteAll();
    }

    @Test
    @Transactional
    void fullOnboardingFlowPersistsUserAndSchool() {
        OnboardingOutcome first = onboardingService.handle(111L, "Today's timetable changed");

        assertTrue(first.newUser());
        assertEquals(OnboardingStatus.PENDING_SCHOOL,
                appUserRepository.findByTelegramChatId(111L).orElseThrow().getOnboardingStatus());

        OnboardingOutcome second = onboardingService.handle(111L, "Green Valley School");

        assertFalse(second.newUser());
        assertTrue(second.pending());
        AppUser afterSchool = appUserRepository.findByTelegramChatId(111L).orElseThrow();
        assertEquals(OnboardingStatus.PENDING_CITY, afterSchool.getOnboardingStatus());
        assertEquals("Green Valley School", afterSchool.getSchool().getName());

        onboardingService.handle(111L, "Bengaluru");

        OnboardingOutcome finalReply = onboardingService.handle(111L, "KA-123");

        AppUser done = appUserRepository.findByTelegramChatId(111L).orElseThrow();
        assertEquals(OnboardingStatus.ACTIVE, done.getOnboardingStatus());
        assertEquals("Bengaluru", done.getSchool().getCity());
        assertEquals("KA-123", done.getSchool().getRegisteredNumber());
        assertNotNull(done.getOnboardedAt());
        assertTrue(finalReply.text().contains("Green Valley School"));
        assertTrue(finalReply.text().contains("Bengaluru"));
        assertTrue(finalReply.text().contains("KA-123"));
    }

    @Test
    @Transactional
    void sameSchoolNameIsReusedAcrossUsers() {
        onboardingService.handle(111L, "first");
        onboardingService.handle(111L, "Green Valley School");

        onboardingService.handle(222L, "second");
        onboardingService.handle(222L, "GREEN VALLEY SCHOOL");

        School shared = schoolRepository.findByNameIgnoreCase("green valley school").orElseThrow();
        assertEquals("Green Valley School", shared.getName());
        assertEquals(1, schoolRepository.findAll().size());
        AppUser firstUser = appUserRepository.findByTelegramChatId(111L).orElseThrow();
        AppUser secondUser = appUserRepository.findByTelegramChatId(222L).orElseThrow();
        assertEquals(shared.getId(), firstUser.getSchool().getId());
        assertEquals(shared.getId(), secondUser.getSchool().getId());
    }

    @Test
    @Transactional
    void skipSchoolNameStaysAnonymousAndActivated() {
        onboardingService.handle(111L, "first");

        OnboardingOutcome outcome = onboardingService.handle(111L, "skip");

        AppUser done = appUserRepository.findByTelegramChatId(111L).orElseThrow();
        assertEquals(OnboardingStatus.ACTIVE, done.getOnboardingStatus());
        assertNull(done.getSchool());
        assertTrue(outcome.text().toLowerCase().contains("anonymous"));
    }

    @Test
    @Transactional
    void restartSurvivesBecauseStateIsPersisted() {
        onboardingService.handle(111L, "first");
        onboardingService.handle(111L, "Green Valley School");

        OnboardingOutcome afterReload = onboardingService.handle(111L, "Mumbai");

        assertEquals(OnboardingStatus.PENDING_REG_NO,
                appUserRepository.findByTelegramChatId(111L).orElseThrow().getOnboardingStatus());
        assertEquals("Mumbai",
                appUserRepository.findByTelegramChatId(111L).orElseThrow().getSchool().getCity());
        assertEquals(OnboardingService.REG_NO_PROMPT, afterReload.text());
    }

    @Test
    @Transactional
    void activeUserIsNotOnboardedAgain() {
        onboardingService.handle(111L, "first");
        onboardingService.handle(111L, "Green Valley School");
        onboardingService.handle(111L, "Mumbai");
        onboardingService.handle(111L, "KA-123");

        OnboardingOutcome outcome = onboardingService.handle(111L, "Any message");

        assertFalse(outcome.newUser());
        assertFalse(outcome.pending());
        assertNull(outcome.text());
        assertEquals(Optional.of(OnboardingStatus.ACTIVE),
                appUserRepository.findByTelegramChatId(111L)
                        .map(AppUser::getOnboardingStatus));
    }
}
