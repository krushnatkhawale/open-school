package com.kaushalya.digitalschool.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.kaushalya.digitalschool.shared.CommunicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiChatServiceImplTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.PromptUserSpec userSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ExtractionRuleService extractionRuleService;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenAnswer(invocation -> {
            Consumer<ChatClient.PromptUserSpec> consumer = invocation.getArgument(0);
            consumer.accept(userSpec);
            return requestSpec;
        });
        when(requestSpec.options(any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(userSpec.text(anyString())).thenReturn(userSpec);
        when(userSpec.media(any(MimeType.class), any(Resource.class))).thenReturn(userSpec);
        when(extractionRuleService.systemPromptFor(any(ExtractionField.class)))
                .thenAnswer(invocation -> {
                    ExtractionField field = invocation.getArgument(0);
                    return field.prompt(field.defaultRules());
                });

        aiChatService = new AiChatServiceImpl(chatClientBuilder, "qwen2.5vl", extractionRuleService);
    }

    @Test
    void classifyReturnsKnownCategory() {
        when(callResponseSpec.content()).thenReturn("DAILY_LESSON_UPDATE");

        assertEquals(CommunicationType.DAILY_LESSON_UPDATE, aiChatService.classify("Today we learned algebra"));
    }

    @Test
    void classifyReturnsHomeworkCategory() {
        when(callResponseSpec.content()).thenReturn("HOMEWORK_ASSIGNMENT");

        assertEquals(CommunicationType.HOMEWORK_ASSIGNMENT, aiChatService.classify("Complete exercise 5.2 by Friday"));
    }

    @Test
    void classifyReturnsCircularNotice() {
        when(callResponseSpec.content()).thenReturn("CIRCULAR_NOTICE");

        assertEquals(CommunicationType.CIRCULAR_NOTICE, aiChatService.classify("School will remain closed on Monday"));
    }

    @Test
    void classifyReturnsActionRequired() {
        when(callResponseSpec.content()).thenReturn("ACTION_REQUIRED");

        assertEquals(CommunicationType.ACTION_REQUIRED, aiChatService.classify("Please sign the permission slip and return"));
    }

    @Test
    void classifyReturnsTimetableChange() {
        when(callResponseSpec.content()).thenReturn("TIMETABLE_CHANGE");

        assertEquals(CommunicationType.TIMETABLE_CHANGE, aiChatService.classify("Tomorrow's periods are shifted"));
    }

    @Test
    void classifyReturnsEvent() {
        when(callResponseSpec.content()).thenReturn("EVENT");

        assertEquals(CommunicationType.EVENT, aiChatService.classify("Annual day celebration on Dec 15"));
    }

    @Test
    void classifyReturnsExamAssessment() {
        when(callResponseSpec.content()).thenReturn("EXAM_ASSESSMENT");

        assertEquals(CommunicationType.EXAM_ASSESSMENT, aiChatService.classify("Mid-term exams start next week"));
    }

    @Test
    void classifyReturnsAttendance() {
        when(callResponseSpec.content()).thenReturn("ATTENDANCE");

        assertEquals(CommunicationType.ATTENDANCE, aiChatService.classify("Your child was absent yesterday"));
    }

    @Test
    void classifyReturnsAchievement() {
        when(callResponseSpec.content()).thenReturn("ACHIEVEMENT");

        assertEquals(CommunicationType.ACHIEVEMENT, aiChatService.classify("Your child won first place in science fair"));
    }

    @Test
    void classifyFallsBackToGeneralInformationForUnknownCategory() {
        when(callResponseSpec.content()).thenReturn("SOME_UNKNOWN_TYPE");

        assertEquals(CommunicationType.GENERAL_INFORMATION, aiChatService.classify("Just a friendly reminder"));
    }

    @Test
    void classifyFallsBackToGeneralInformationForNullResponse() {
        when(callResponseSpec.content()).thenReturn(null);

        assertEquals(CommunicationType.GENERAL_INFORMATION, aiChatService.classify("Hello"));
    }

    @Test
    void classifyFallsBackToGeneralInformationForBlankResponse() {
        when(callResponseSpec.content()).thenReturn("   ");

        assertEquals(CommunicationType.GENERAL_INFORMATION, aiChatService.classify("Hello"));
    }

    @Test
    void classifyTrimsResponseBeforeParsing() {
        when(callResponseSpec.content()).thenReturn("  ACTION_REQUIRED  ");

        assertEquals(CommunicationType.ACTION_REQUIRED, aiChatService.classify("Please sign this"));
    }

    @Test
    void classifyWithImageAndCaptionReturnsCategory() {
        when(callResponseSpec.content()).thenReturn("EVENT");
        byte[] imageData = new byte[]{1, 2, 3};

        assertEquals(CommunicationType.EVENT,
                aiChatService.classify("Annual day event", imageData, "image/jpeg"));
    }

    @Test
    void classifyWithImageAndNullCaptionReturnsCategory() {
        when(callResponseSpec.content()).thenReturn("CIRCULAR_NOTICE");
        byte[] imageData = new byte[]{1, 2, 3};

        assertEquals(CommunicationType.CIRCULAR_NOTICE,
                aiChatService.classify(null, imageData, "image/png"));
    }

    @Test
    void classifyWithImageFallsBackOnUnknownResponse() {
        when(callResponseSpec.content()).thenReturn("BOGUS_TYPE");
        byte[] imageData = new byte[]{1, 2, 3};

        assertEquals(CommunicationType.GENERAL_INFORMATION,
                aiChatService.classify("Message", imageData, "image/jpeg"));
    }

    @Test
    void classifyWithImageHandlesEmptyResponse() {
        when(callResponseSpec.content()).thenReturn("");
        byte[] imageData = new byte[]{1, 2, 3};

        assertEquals(CommunicationType.GENERAL_INFORMATION,
                aiChatService.classify("Message", imageData, "image/jpeg"));
    }

    @Test
    void classifyWithImageAndMimeTypeDetection() {
        when(callResponseSpec.content()).thenReturn("ACTION_REQUIRED");
        byte[] imageData = new byte[]{1, 2, 3};

        assertEquals(CommunicationType.ACTION_REQUIRED,
                aiChatService.classify("Please sign", imageData, "image/webp"));
    }

    @Test
    void describeImageReturnsDescription() {
        when(callResponseSpec.content()).thenReturn("A photo of a school bus");
        byte[] imageData = new byte[]{1, 2, 3};

        String result = aiChatService.describeImage(imageData, "image/jpeg");
        assertEquals("A photo of a school bus", result);
    }

    @Test
    void describeImageTrimsResponse() {
        when(callResponseSpec.content()).thenReturn("  A classroom with students  ");
        byte[] imageData = new byte[]{1, 2, 3};

        String result = aiChatService.describeImage(imageData, "image/png");
        assertEquals("A classroom with students", result);
    }

    @Test
    void describeImageReturnsFallbackForNullResponse() {
        when(callResponseSpec.content()).thenReturn(null);
        byte[] imageData = new byte[]{1, 2, 3};

        String result = aiChatService.describeImage(imageData, "image/jpeg");
        assertEquals("Could not describe the image.", result);
    }

    @Test
    void describeImageReturnsFallbackForNullImageData() {
        String result = aiChatService.describeImage(null, "image/jpeg");
        assertEquals("No image data available.", result);
    }

    @Test
    void describeImageReturnsFallbackForNullMediaType() {
        byte[] imageData = new byte[]{1, 2, 3};
        String result = aiChatService.describeImage(imageData, null);
        assertEquals("No image data available.", result);
    }

    @Test
    void extractDateNormalizesWrongCenturyYear() {
        when(callResponseSpec.content()).thenReturn("1926-07-29");

        assertEquals("2026-07-29", aiChatService.extractDate("Lesson date 29/07/26"));
    }

    @Test
    void extractDateKeepsCurrentCenturyYear() {
        when(callResponseSpec.content()).thenReturn("2026-07-30");

        assertEquals("2026-07-30", aiChatService.extractDate("Lesson date 30/07/26"));
    }

    @Test
    void extractDateReturnsNullForNone() {
        when(callResponseSpec.content()).thenReturn("NONE");

        assertEquals(null, aiChatService.extractDate("No date in this note"));
    }

    @Test
    void extractDateReturnsNullForBlankInput() {
        assertEquals(null, aiChatService.extractDate(null));
        assertEquals(null, aiChatService.extractDate("   "));
    }

    @Test
    void extractTagsParsesCommaSeparatedResponse() {
        when(callResponseSpec.content()).thenReturn("story, hindi");

        assertEquals(List.of("story", "hindi"), aiChatService.extractTags("A short story in Hindi about the lion"));
    }

    @Test
    void extractTagsTrimsLowercasesAndDeduplicates() {
        when(callResponseSpec.content()).thenReturn(" Story , HINDI , story , maths ");

        assertEquals(List.of("story", "hindi", "maths"), aiChatService.extractTags("lesson text"));
    }

    @Test
    void extractTagsReturnsEmptyForNone() {
        when(callResponseSpec.content()).thenReturn("NONE");

        assertEquals(List.of(), aiChatService.extractTags("Fee reminder"));
    }

    @Test
    void extractTagsReturnsEmptyForBlankResponse() {
        when(callResponseSpec.content()).thenReturn("   ");

        assertEquals(List.of(), aiChatService.extractTags("Fee reminder"));
    }

    @Test
    void extractTagsReturnsEmptyForBlankInput() {
        assertEquals(List.of(), aiChatService.extractTags(null));
        assertEquals(List.of(), aiChatService.extractTags("   "));
    }

    @Test
    void extractDatePromptPrefersTeacherRedBottomDate() {
        AtomicReference<String> captured = new AtomicReference<>();
        when(requestSpec.system(anyString())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(callResponseSpec.content()).thenReturn("2026-07-23");

        aiChatService.extractDate("A notebook page with dates in pencil and red ink");

        String prompt = captured.get();
        assertNotNull(prompt);
        assertTrue(prompt.contains("red bottom date"));
        assertTrue(prompt.contains("teacher"));
        assertTrue(prompt.contains("fall back to the pencil date"));
    }

    @Test
    void describeImagePromptCapturesTeacherRedBottomDate() {
        AtomicReference<String> captured = new AtomicReference<>();
        when(requestSpec.system(anyString())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(callResponseSpec.content()).thenReturn("A notebook page");
        byte[] imageData = new byte[]{1, 2, 3};

        aiChatService.describeImage(imageData, "image/jpeg");

        String prompt = captured.get();
        assertNotNull(prompt);
        assertTrue(prompt.contains("bottom in red ink"));
        assertTrue(prompt.contains("teacher's date"));
    }

    @Test
    void classifyUsesServiceProvidedSystemPrompt() {
        AtomicReference<String> captured = new AtomicReference<>();
        when(requestSpec.system(anyString())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(extractionRuleService.systemPromptFor(ExtractionField.CLASSIFICATION))
                .thenReturn("CUSTOM CLASSIFICATION RULES\n\nRespond with ONLY the category name.");
        when(callResponseSpec.content()).thenReturn("ACTION_REQUIRED");

        aiChatService.classify("Please sign this form");

        assertTrue(captured.get().contains("CUSTOM CLASSIFICATION RULES"));
    }

    @Test
    void extractDateUsesServiceProvidedSystemPrompt() {
        AtomicReference<String> captured = new AtomicReference<>();
        when(requestSpec.system(anyString())).thenAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(extractionRuleService.systemPromptFor(ExtractionField.DATE))
                .thenReturn("CUSTOM DATE RULES\n\nReturn ONLY the date in YYYY-MM-DD format.");
        when(callResponseSpec.content()).thenReturn("2026-08-01");

        aiChatService.extractDate("lesson note");

        assertTrue(captured.get().contains("CUSTOM DATE RULES"));
    }

    @Test
    void describeImageUsesServiceSystemPromptAndFieldUserInstruction() {
        AtomicReference<String> capturedSystem = new AtomicReference<>();
        AtomicReference<String> capturedUser = new AtomicReference<>();
        when(requestSpec.system(anyString())).thenAnswer(invocation -> {
            capturedSystem.set(invocation.getArgument(0));
            return requestSpec;
        });
        when(userSpec.text(anyString())).thenAnswer(invocation -> {
            capturedUser.set(invocation.getArgument(0));
            return userSpec;
        });
        when(callResponseSpec.content()).thenReturn("A notebook page");

        aiChatService.describeImage(new byte[]{1, 2, 3}, "image/jpeg");

        assertNotNull(capturedSystem.get());
        assertNotNull(capturedUser.get());
        assertTrue(capturedSystem.get().contains("bottom in red ink"));
        assertTrue(capturedUser.get().contains("Describe everything visible in this image"));
    }
}
