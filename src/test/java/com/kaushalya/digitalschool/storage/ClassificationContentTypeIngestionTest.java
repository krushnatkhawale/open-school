package com.kaushalya.digitalschool.storage;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.shared.ClassificationEvent;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:ctingestiontest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ClassificationContentTypeIngestionTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            return Mockito.mock(AiChatService.class);
        }
    }

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private ClassificationRecordRepository repository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void listenerStoresImageContentTypeForImageEvents() {
        publisher.publishEvent(new ClassificationEvent(
                111L, "caption", CommunicationType.DAILY_LESSON_UPDATE, "description",
                LocalDate.now(), Instant.now(), new byte[]{1, 2, 3}, "image/jpeg"));

        List<ClassificationRecord> records = repository.findAll();
        assertEquals(1, records.size());
        assertEquals(ContentType.IMAGE, records.get(0).getContentType());
    }

    @Test
    void listenerStoresTextContentTypeForTextEvents() {
        publisher.publishEvent(new ClassificationEvent(
                111L, "plain text message", CommunicationType.REMINDER, null));

        List<ClassificationRecord> records = repository.findAll();
        assertEquals(1, records.size());
        assertEquals(ContentType.TEXT, records.get(0).getContentType());
    }

    @Test
    void idStaysUniqueAcrossIngestedRecords() {
        publisher.publishEvent(new ClassificationEvent(
                111L, "one", CommunicationType.REMINDER, null, null, Instant.now()));
        publisher.publishEvent(new ClassificationEvent(
                222L, "two", CommunicationType.REMINDER, null, null, Instant.now()));

        List<ClassificationRecord> records = repository.findAll();
        assertEquals(2, records.size());
        assertEquals(2, records.stream().map(ClassificationRecord::getId).distinct().count());
    }
}
