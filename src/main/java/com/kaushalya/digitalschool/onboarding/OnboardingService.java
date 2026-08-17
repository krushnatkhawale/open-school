package com.kaushalya.digitalschool.onboarding;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OnboardingService {

    static final String SKIP = "skip";
    public static final String SCHOOL_PROMPT =
            "Welcome! What is the name of the school you represent? (reply \"skip\" to stay anonymous)";
    public static final String CITY_PROMPT =
            "Which city is the school in? (optional, reply \"skip\" to skip)";
    public static final String REG_NO_PROMPT =
            "Do you have a school registration number? (optional, reply \"skip\" to skip)";
    public static final String ANONYMOUS_CONFIRMATION =
            "You're all set! You chose to stay anonymous.";

    private final AppUserRepository appUserRepository;
    private final SchoolRepository schoolRepository;

    public OnboardingService(AppUserRepository appUserRepository, SchoolRepository schoolRepository) {
        this.appUserRepository = appUserRepository;
        this.schoolRepository = schoolRepository;
    }

    public boolean isOnboarding(Long chatId) {
        AppUser user = appUserRepository.findByTelegramChatId(chatId).orElse(null);
        return user == null || user.getOnboardingStatus() != OnboardingStatus.ACTIVE;
    }

    @Transactional
    public OnboardingOutcome handle(Long chatId, String textOrNull) {
        AppUser user = appUserRepository.findByTelegramChatId(chatId).orElse(null);
        if (user == null) {
            AppUser created = new AppUser(UUID.randomUUID(), chatId, UserRole.SCHOOL,
                    OnboardingStatus.PENDING_SCHOOL, Instant.now());
            appUserRepository.save(created);
            return OnboardingOutcome.started(SCHOOL_PROMPT);
        }
        return advance(user, textOrNull);
    }

    private OnboardingOutcome advance(AppUser user, String textOrNull) {
        String answer = textOrNull == null ? null : textOrNull.trim();
        boolean textAnswer = answer != null && !answer.isEmpty();

        switch (user.getOnboardingStatus()) {
            case PENDING_SCHOOL -> {
                if (!textAnswer) {
                    return OnboardingOutcome.continued(SCHOOL_PROMPT);
                }
                if (isSkip(answer)) {
                    user.setOnboardingStatus(OnboardingStatus.ACTIVE);
                    user.setOnboardedAt(Instant.now());
                    return OnboardingOutcome.continued(ANONYMOUS_CONFIRMATION);
                }
                School school = schoolRepository.findByNameIgnoreCase(answer)
                        .orElseGet(() -> schoolRepository.save(
                                new School(UUID.randomUUID(), answer, null, null, Instant.now())));
                user.setSchool(school);
                user.setOnboardingStatus(OnboardingStatus.PENDING_CITY);
                return OnboardingOutcome.continued(CITY_PROMPT);
            }
            case PENDING_CITY -> {
                if (!textAnswer) {
                    return OnboardingOutcome.continued(CITY_PROMPT);
                }
                if (!isSkip(answer) && user.getSchool() != null) {
                    user.getSchool().setCity(answer);
                }
                user.setOnboardingStatus(OnboardingStatus.PENDING_REG_NO);
                return OnboardingOutcome.continued(REG_NO_PROMPT);
            }
            case PENDING_REG_NO -> {
                if (!textAnswer) {
                    return OnboardingOutcome.continued(REG_NO_PROMPT);
                }
                if (!isSkip(answer) && user.getSchool() != null) {
                    user.getSchool().setRegisteredNumber(answer);
                }
                user.setOnboardingStatus(OnboardingStatus.ACTIVE);
                user.setOnboardedAt(Instant.now());
                return OnboardingOutcome.continued(confirmation(user));
            }
            case ACTIVE -> {
                return OnboardingOutcome.active();
            }
            default -> throw new IllegalStateException(
                    "Unexpected onboarding status " + user.getOnboardingStatus());
        }
    }

    private static boolean isSkip(String answer) {
        return answer.equalsIgnoreCase(SKIP);
    }

    private static String confirmation(AppUser user) {
        School school = user.getSchool();
        if (school == null || school.getName() == null || school.getName().isBlank()) {
            return ANONYMOUS_CONFIRMATION;
        }
        String details = school.getName();
        if (school.getCity() != null && !school.getCity().isBlank()) {
            details += ", " + school.getCity();
        }
        if (school.getRegisteredNumber() != null && !school.getRegisteredNumber().isBlank()) {
            details += " (reg. no " + school.getRegisteredNumber() + ")";
        }
        return "You're all set — " + details + " is onboarded!";
    }
}
