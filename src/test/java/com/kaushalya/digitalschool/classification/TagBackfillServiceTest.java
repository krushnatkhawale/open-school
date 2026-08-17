package com.kaushalya.digitalschool.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import com.kaushalya.digitalschool.storage.TagBackfillCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TagBackfillServiceTest {

    @Mock
    private ClassificationRecordRepository repository;

    @Mock
    private AiChatService aiChatService;

    private TagBackfillService service;

    @BeforeEach
    void setUp() {
        service = new TagBackfillService(repository, aiChatService);
    }

    @Test
    void textToTagUsesOriginalTextWhenNoDescription() {
        assertEquals("Homework: fraction sums", TagBackfillService.textToTag("Homework: fraction sums", null));
    }

    @Test
    void textToTagUsesDescriptionWhenNoText() {
        assertEquals("A notebook page in Hindi", TagBackfillService.textToTag("  ", "A notebook page in Hindi"));
    }

    @Test
    void textToTagCombinesTextAndDescription() {
        assertEquals("Caption\nDescription",
                TagBackfillService.textToTag("Caption", "Description"));
    }

    @Test
    void textToTagReturnsNullWhenBothMissing() {
        assertNull(TagBackfillService.textToTag(null, null));
        assertNull(TagBackfillService.textToTag("   ", null));
        assertNull(TagBackfillService.textToTag(null, "  "));
    }

    @Test
    void runExtractsTagsAndPersistsThem() {
        UUID id = UUID.randomUUID();
        when(repository.findTagBackfillCandidates())
                .thenReturn(List.of(new TagBackfillCandidate(id, "A short story about a lion", null)));
        when(aiChatService.extractTags("A short story about a lion")).thenReturn(List.of("story", "hindi"));

        TagBackfillResult result = service.run();

        assertEquals(1, result.candidates());
        assertEquals(1, result.tagged());
        assertEquals(0, result.noTags());
        assertEquals(0, result.skipped());
        assertEquals(0, result.failed());
        assertFalse(result.alreadyRunning());
        verify(repository).updateTags(eq(id), eq("story,hindi"));
    }

    @Test
    void runCountsNoTagResultsWithoutPersisting() {
        UUID id = UUID.randomUUID();
        when(repository.findTagBackfillCandidates())
                .thenReturn(List.of(new TagBackfillCandidate(id, "Fee payment reminder", null)));
        when(aiChatService.extractTags(anyString())).thenReturn(List.of());

        TagBackfillResult result = service.run();

        assertEquals(1, result.noTags());
        assertEquals(0, result.tagged());
        verify(repository, never()).updateTags(org.mockito.ArgumentMatchers.any(), anyString());
    }

    @Test
    void runSkipsRecordsWithoutText() {
        UUID id = UUID.randomUUID();
        when(repository.findTagBackfillCandidates())
                .thenReturn(List.of(new TagBackfillCandidate(id, null, null)));

        TagBackfillResult result = service.run();

        assertEquals(1, result.skipped());
        assertEquals(0, result.tagged());
        verify(aiChatService, never()).extractTags(anyString());
    }

    @Test
    void runCountsFailuresAndContinues() {
        UUID goodId = UUID.randomUUID();
        UUID badId = UUID.randomUUID();
        when(repository.findTagBackfillCandidates())
                .thenReturn(List.of(
                        new TagBackfillCandidate(badId, "text that breaks", null),
                        new TagBackfillCandidate(goodId, "normal text", null)));
        when(aiChatService.extractTags("text that breaks")).thenThrow(new RuntimeException("ollama down"));
        when(aiChatService.extractTags("normal text")).thenReturn(List.of("maths"));

        TagBackfillResult result = service.run();

        assertEquals(2, result.candidates());
        assertEquals(1, result.tagged());
        assertEquals(1, result.failed());
        verify(repository).updateTags(eq(goodId), eq("maths"));
        verify(repository, never()).updateTags(eq(badId), anyString());
    }

    @Test
    void concurrentRunIsRejectedWhileFirstIsInProgress() throws Exception {
        UUID id = UUID.randomUUID();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        when(repository.findTagBackfillCandidates())
                .thenReturn(List.of(new TagBackfillCandidate(id, "some content", null)));
        when(aiChatService.extractTags(anyString())).thenAnswer(invocation -> {
            firstStarted.countDown();
            releaseFirst.await(10, TimeUnit.SECONDS);
            return List.of("story");
        });

        Thread first = new Thread(() -> service.run());
        first.start();
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS));

        TagBackfillResult second = service.run();
        assertTrue(second.alreadyRunning());
        assertEquals(0, second.candidates());

        releaseFirst.countDown();
        first.join(5000);
        assertFalse(first.isAlive());
    }

    @Test
    void inProgressFactoryFlagsRejectedRun() {
        TagBackfillResult result = TagBackfillResult.inProgress();
        assertTrue(result.alreadyRunning());
        assertEquals(0, result.candidates());
    }
}
