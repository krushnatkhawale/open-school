package com.kaushalya.digitalschool.onboarding;

public record OnboardingOutcome(boolean newUser, boolean pending, String text) {

    public static OnboardingOutcome active() {
        return new OnboardingOutcome(false, false, null);
    }

    public static OnboardingOutcome started(String prompt) {
        return new OnboardingOutcome(true, true, prompt);
    }

    public static OnboardingOutcome continued(String prompt) {
        return new OnboardingOutcome(false, true, prompt);
    }
}
