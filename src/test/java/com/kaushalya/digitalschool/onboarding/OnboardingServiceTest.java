package com.kaushalya.digitalschool.onboarding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class OnboardingServiceTest {

    private static final long CHAT_ID = 111L;

    private AppUserRepository appUserRepository;
    private SchoolRepository schoolRepository;
    private OnboardingService service;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(schoolRepository.save(any(School.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new OnboardingService(appUserRepository, schoolRepository);
    }

    @Test
    void unknownChatCreatesPendingSchoolUserAndPrompts() {
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.empty());

        OnboardingOutcome outcome = service.handle(CHAT_ID, "First message");

        assertTrue(outcome.newUser());
        assertTrue(outcome.pending());
        assertEquals(OnboardingService.SCHOOL_PROMPT, outcome.text());
        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals(OnboardingStatus.PENDING_SCHOOL, saved.getOnboardingStatus());
        assertEquals(UserRole.SCHOOL, saved.getRole());
        assertEquals(CHAT_ID, saved.getTelegramChatId());
    }

    @Test
    void schoolNameAnswerCreatesSchoolAndAsksCity() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_SCHOOL);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));
        when(schoolRepository.findByNameIgnoreCase("Green Valley")).thenReturn(Optional.empty());

        OnboardingOutcome outcome = service.handle(CHAT_ID, "Green Valley");

        assertFalse(outcome.newUser());
        assertTrue(outcome.pending());
        assertEquals(OnboardingService.CITY_PROMPT, outcome.text());
        assertEquals(OnboardingStatus.PENDING_CITY, user.getOnboardingStatus());
        assertEquals("Green Valley", user.getSchool().getName());
    }

    @Test
    void existingSchoolIsReusedCaseInsensitively() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_SCHOOL);
        School existing = new School(UUID.randomUUID(), "green valley", null, null, Instant.now());
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));
        when(schoolRepository.findByNameIgnoreCase("Green Valley")).thenReturn(Optional.of(existing));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "Green Valley");

        assertFalse(outcome.newUser());
        assertEquals(OnboardingService.CITY_PROMPT, outcome.text());
        assertEquals(existing, user.getSchool());
        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void cityAnswerSetsCityAndAsksRegNo() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_CITY);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "Bengaluru");

        assertFalse(outcome.newUser());
        assertTrue(outcome.pending());
        assertEquals(OnboardingService.REG_NO_PROMPT, outcome.text());
        assertEquals(OnboardingStatus.PENDING_REG_NO, user.getOnboardingStatus());
        assertEquals("Bengaluru", user.getSchool().getCity());
    }

    @Test
    void skipCityLeavesCityNullAndAsksRegNo() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_CITY);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "skip");

        assertFalse(outcome.newUser());
        assertTrue(outcome.pending());
        assertEquals(OnboardingService.REG_NO_PROMPT, outcome.text());
        assertEquals(OnboardingStatus.PENDING_REG_NO, user.getOnboardingStatus());
        assertNull(user.getSchool().getCity());
    }

    @Test
    void regNoAnswerActivatesWithConfirmation() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_REG_NO);
        user.getSchool().setCity("Bengaluru");
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "KA-123");

        assertFalse(outcome.newUser());
        assertTrue(outcome.pending());
        assertEquals(OnboardingStatus.ACTIVE, user.getOnboardingStatus());
        assertTrue(outcome.text().contains("Green Valley"));
        assertTrue(outcome.text().contains("KA-123"));
        assertTrue(outcome.text().contains("Bengaluru"));
        assertEquals("KA-123", user.getSchool().getRegisteredNumber());
    }

    @Test
    void skipRegNoActivatesAnonymousSchoolWithoutRegNo() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_REG_NO);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "skip");

        assertEquals(OnboardingStatus.ACTIVE, user.getOnboardingStatus());
        assertNull(user.getSchool().getRegisteredNumber());
        assertTrue(outcome.text().contains("Green Valley"));
    }

    @Test
    void skipSchoolNameActivatesFullyAnonymous() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_SCHOOL);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "skip");

        assertEquals(OnboardingStatus.ACTIVE, user.getOnboardingStatus());
        assertNull(user.getSchool());
        assertEquals(OnboardingService.ANONYMOUS_CONFIRMATION, outcome.text());
        verify(schoolRepository, never()).save(any(School.class));
    }

    @Test
    void skipIsCaseInsensitive() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_SCHOOL);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        service.handle(CHAT_ID, "SKIP");

        assertEquals(OnboardingStatus.ACTIVE, user.getOnboardingStatus());
        assertNull(user.getSchool());
    }

    @Test
    void nonTextWhilePendingReasksCurrentPrompt() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_CITY);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, null);

        assertFalse(outcome.newUser());
        assertTrue(outcome.pending());
        assertEquals(OnboardingService.CITY_PROMPT, outcome.text());
        assertEquals(OnboardingStatus.PENDING_CITY, user.getOnboardingStatus());
    }

    @Test
    void blankTextWhilePendingReasksCurrentPrompt() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_REG_NO);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "   ");

        assertEquals(OnboardingService.REG_NO_PROMPT, outcome.text());
        assertEquals(OnboardingStatus.PENDING_REG_NO, user.getOnboardingStatus());
    }

    @Test
    void unknownChatIsOnboarding() {
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.empty());

        assertTrue(service.isOnboarding(CHAT_ID));
    }

    @Test
    void pendingChatIsOnboarding() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_CITY);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        assertTrue(service.isOnboarding(CHAT_ID));
    }

    @Test
    void activeChatIsNotOnboarding() {
        AppUser user = pendingUser(OnboardingStatus.ACTIVE);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        assertFalse(service.isOnboarding(CHAT_ID));
    }

    @Test
    void activeUserReturnsNoPrompt() {
        AppUser user = pendingUser(OnboardingStatus.ACTIVE);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "anything");

        assertFalse(outcome.newUser());
        assertFalse(outcome.pending());
        assertNull(outcome.text());
    }

    @Test
    void pendingStateLoadedFromDbContinuesWhereItLeftOff() {
        AppUser user = pendingUser(OnboardingStatus.PENDING_CITY);
        when(appUserRepository.findByTelegramChatId(CHAT_ID)).thenReturn(Optional.of(user));

        OnboardingOutcome outcome = service.handle(CHAT_ID, "Pune");

        assertEquals(OnboardingStatus.PENDING_REG_NO, user.getOnboardingStatus());
        assertEquals("Pune", user.getSchool().getCity());
        assertEquals(OnboardingService.REG_NO_PROMPT, outcome.text());
    }

    private AppUser pendingUser(OnboardingStatus status) {
        AppUser user = new AppUser(UUID.randomUUID(), CHAT_ID, UserRole.SCHOOL, status, Instant.now());
        if (status != OnboardingStatus.PENDING_SCHOOL) {
            user.setSchool(new School(UUID.randomUUID(), "Green Valley", null, null, Instant.now()));
        }
        return user;
    }
}
