package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.classification.ContentTypeBackfillResult;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:ctbackfillctrltest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.content-type-backfill.enabled=true"
})
class ContentTypeBackfillControllerTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            return Mockito.mock(AiChatService.class);
        }
    }

    @Autowired
    private ContentTypeBackfillController controller;

    @Autowired
    private ClassificationRecordRepository repository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void endpointBeanExistsWhenEnabled() {
        assertNotNull(controller);
    }

    @Test
    void runTriggersBackfillAndPersistsContentTypes() {
        ClassificationRecord image = new ClassificationRecord(UUID.randomUUID(), 111L, "caption",
                CommunicationType.DAILY_LESSON_UPDATE, "desc",
                LocalDate.now(), Instant.now());
        image.setImageData(new byte[]{1, 2, 3});
        image.setImageContentType("image/jpeg");
        ClassificationRecord text = new ClassificationRecord(UUID.randomUUID(), 111L, "plain text",
                CommunicationType.REMINDER, null, LocalDate.now(), Instant.now());
        repository.saveAll(List.of(image, text));

        ContentTypeBackfillResult result = controller.run();

        assertEquals(2, result.candidates());
        assertEquals(2, result.updated());
        assertEquals(false, result.alreadyRunning());

        assertEquals(ContentType.IMAGE, repository.findById(image.getId()).orElseThrow().getContentType());
        assertEquals(ContentType.TEXT, repository.findById(text.getId()).orElseThrow().getContentType());
    }
}
