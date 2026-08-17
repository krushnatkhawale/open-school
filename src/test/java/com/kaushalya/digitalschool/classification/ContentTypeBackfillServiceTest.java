package com.kaushalya.digitalschool.classification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kaushalya.digitalschool.shared.ContentType;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentTypeBackfillServiceTest {

    @Mock
    private ClassificationRecordRepository repository;

    private ContentTypeBackfillService service;

    @BeforeEach
    void setUp() {
        service = new ContentTypeBackfillService(repository);
    }

    @Test
    void runBackfillsAllMissingContentTypes() {
        when(repository.countMissingContentTypes()).thenReturn(2L);
        when(repository.backfillMissingContentTypes(ContentType.IMAGE, ContentType.TEXT)).thenReturn(2);

        ContentTypeBackfillResult result = service.run();

        assertEquals(2, result.candidates());
        assertEquals(2, result.updated());
        assertEquals(0, result.failed());
        assertFalse(result.alreadyRunning());
        verify(repository).backfillMissingContentTypes(ContentType.IMAGE, ContentType.TEXT);
    }

    @Test
    void runCountsRowsNotMatchedAsFailed() {
        when(repository.countMissingContentTypes()).thenReturn(3L);
        when(repository.backfillMissingContentTypes(ContentType.IMAGE, ContentType.TEXT)).thenReturn(2);

        ContentTypeBackfillResult result = service.run();

        assertEquals(3, result.candidates());
        assertEquals(2, result.updated());
        assertEquals(1, result.failed());
    }

    @Test
    void runDoesNothingWhenNoCandidates() {
        when(repository.countMissingContentTypes()).thenReturn(0L);

        ContentTypeBackfillResult result = service.run();

        assertEquals(0, result.candidates());
        assertEquals(0, result.updated());
        verify(repository, never()).backfillMissingContentTypes(any(), any());
    }

    @Test
    void concurrentRunIsRejectedWhileFirstIsInProgress() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        when(repository.countMissingContentTypes()).thenAnswer(invocation -> {
            firstStarted.countDown();
            releaseFirst.await(10, TimeUnit.SECONDS);
            return 1L;
        });

        Thread first = new Thread(() -> service.run());
        first.start();
        assertTrue(firstStarted.await(5, TimeUnit.SECONDS));

        ContentTypeBackfillResult second = service.run();
        assertTrue(second.alreadyRunning());
        assertEquals(0, second.candidates());

        releaseFirst.countDown();
        first.join(5000);
        assertFalse(first.isAlive());
    }

    @Test
    void inProgressFactoryFlagsRejectedRun() {
        ContentTypeBackfillResult result = ContentTypeBackfillResult.inProgress();
        assertTrue(result.alreadyRunning());
        assertEquals(0, result.candidates());
    }
}
