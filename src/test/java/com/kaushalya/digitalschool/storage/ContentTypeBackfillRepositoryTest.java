package com.kaushalya.digitalschool.storage;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:ctbackfilltest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ContentTypeBackfillRepositoryTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            return Mockito.mock(AiChatService.class);
        }
    }

    @Autowired
    private ClassificationRecordRepository repository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void countMissingContentTypesReturnsOnlyUnsetRows() {
        repository.saveAll(List.of(
                recordWithImage(),
                recordWithText(),
                recordWithContentType(ContentType.IMAGE),
                recordWithContentType(ContentType.TEXT)));

        assertEquals(2, repository.countMissingContentTypes());
    }

    @Test
    @Transactional
    void backfillSetsImageForBlobRowsAndTextForOthers() {
        ClassificationRecord image = recordWithImage();
        ClassificationRecord text = recordWithText();
        repository.saveAll(List.of(image, text));

        int updated = repository.backfillMissingContentTypes(ContentType.IMAGE, ContentType.TEXT);

        assertEquals(2, updated);
        assertEquals(ContentType.IMAGE, repository.findById(image.getId()).orElseThrow().getContentType());
        assertEquals(ContentType.TEXT, repository.findById(text.getId()).orElseThrow().getContentType());
    }

    @Test
    @Transactional
    void backfillIsIdempotent() {
        repository.save(recordWithImage());

        repository.backfillMissingContentTypes(ContentType.IMAGE, ContentType.TEXT);

        assertEquals(0, repository.countMissingContentTypes());
        assertEquals(0, repository.backfillMissingContentTypes(ContentType.IMAGE, ContentType.TEXT));
    }

    private ClassificationRecord recordWithImage() {
        ClassificationRecord record = new ClassificationRecord(UUID.randomUUID(), 111L, "caption",
                CommunicationType.DAILY_LESSON_UPDATE, "description",
                LocalDate.now(), Instant.now());
        record.setImageData(new byte[]{1, 2, 3});
        record.setImageContentType("image/jpeg");
        return record;
    }

    private ClassificationRecord recordWithText() {
        return new ClassificationRecord(UUID.randomUUID(), 111L, "plain text",
                CommunicationType.REMINDER, null, LocalDate.now(), Instant.now());
    }

    private ClassificationRecord recordWithContentType(ContentType contentType) {
        ClassificationRecord record = recordWithText();
        record.setContentType(contentType);
        return record;
    }
}
