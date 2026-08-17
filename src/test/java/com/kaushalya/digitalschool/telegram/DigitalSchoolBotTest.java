package com.kaushalya.digitalschool.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.onboarding.OnboardingOutcome;
import com.kaushalya.digitalschool.onboarding.OnboardingService;
import com.kaushalya.digitalschool.storage.ScheduledEventRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;

class DigitalSchoolBotTest {

    private static final long CHAT_ID = 111L;

    private OnboardingService onboardingService;
    private DigitalSchoolBot bot;

    @BeforeEach
    void setUp() {
        onboardingService = mock(OnboardingService.class);
        bot = new DigitalSchoolBot("test-token",
                mock(AiChatService.class),
                mock(ApplicationEventPublisher.class),
                mock(ScheduledEventRepository.class),
                onboardingService);
    }

    @Test
    void newUserReplyCombinesClassificationAndPrompt() {
        when(onboardingService.handle(CHAT_ID, "Today's timetable changed"))
                .thenReturn(OnboardingOutcome.started(OnboardingService.SCHOOL_PROMPT));

        String reply = bot.onboardingReply(CHAT_ID, "Today's timetable changed", "TIMETABLE_CHANGE");

        assertEquals("TIMETABLE_CHANGE\n\n" + OnboardingService.SCHOOL_PROMPT, reply);
    }

    @Test
    void pendingReplyIsPromptOnly() {
        when(onboardingService.handle(CHAT_ID, "Green Valley School"))
                .thenReturn(OnboardingOutcome.continued(OnboardingService.CITY_PROMPT));

        String reply = bot.onboardingReply(CHAT_ID, "Green Valley School", "HOMEWORK_ASSIGNMENT");

        assertEquals(OnboardingService.CITY_PROMPT, reply);
    }

    @Test
    void activeReplyIsClassificationOnly() {
        when(onboardingService.handle(CHAT_ID, "Today's timetable changed"))
                .thenReturn(OnboardingOutcome.active());

        String reply = bot.onboardingReply(CHAT_ID, "Today's timetable changed", "TIMETABLE_CHANGE");

        assertEquals("TIMETABLE_CHANGE", reply);
    }

    @Test
    void photoWhilePendingRoutesNullTextToOnboarding() {
        when(onboardingService.handle(CHAT_ID, null))
                .thenReturn(OnboardingOutcome.continued(OnboardingService.CITY_PROMPT));

        String reply = bot.onboardingReply(CHAT_ID, null, "EVENT");

        assertEquals(OnboardingService.CITY_PROMPT, reply);
    }

    @Test
    void storedTextKeepsCaptionVerbatim() {
        String description = "The image shows a notebook page.";

        assertEquals("Daily update: maths homework",
                DigitalSchoolBot.storedTextFor("Daily update: maths homework", description));
    }

    @Test
    void storedTextFallsBackToDescriptionForNullCaption() {
        String description = "The image shows a notebook page.";

        assertEquals(description, DigitalSchoolBot.storedTextFor(null, description));
    }

    @Test
    void storedTextFallsBackToDescriptionForBlankCaption() {
        String description = "The image shows a notebook page.";

        assertEquals(description, DigitalSchoolBot.storedTextFor("   ", description));
    }

    @Test
    void largestPhotoPicksLargestFileSize() {
        PhotoSize small = PhotoSize.builder().fileId("small").fileSize(100).build();
        PhotoSize large = PhotoSize.builder().fileId("large").fileSize(5000).build();

        assertEquals(large, DigitalSchoolBot.largestPhoto(List.of(small, large)));
    }

    @Test
    void largestPhotoThrowsSpecificExceptionForEmptyList() {
        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> DigitalSchoolBot.largestPhoto(List.of()));

        assertEquals("No photo sizes available", ex.getMessage());
    }

    @Test
    void botServesAsItsOwnNonDeprecatedUpdateConsumer() {
        assertInstanceOf(LongPollingUpdateConsumer.class, bot);
        assertSame(bot, bot.getUpdatesConsumer());
    }
}
