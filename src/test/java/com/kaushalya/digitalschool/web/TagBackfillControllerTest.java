package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.classification.TagBackfillResult;
import com.kaushalya.digitalschool.shared.CommunicationType;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:backfillctrltest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.tag-backfill.enabled=true"
})
class TagBackfillControllerTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            AiChatService service = Mockito.mock(AiChatService.class);
            when(service.extractTags(anyString())).thenReturn(List.of("story", "hindi"));
            return service;
        }
    }

    @Autowired
    private TagBackfillController controller;

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
    void runTriggersBackfillAndPersistsTags() {
        ClassificationRecord record = new ClassificationRecord(UUID.randomUUID(), 111L, "A story in Hindi",
                CommunicationType.DAILY_LESSON_UPDATE, null, LocalDate.now(), Instant.now());
        repository.save(record);

        TagBackfillResult result = controller.run();

        assertEquals(1, result.candidates());
        assertEquals(1, result.tagged());
        assertEquals(false, result.alreadyRunning());

        ClassificationRecord reloaded = repository.findById(record.getId()).orElseThrow();
        assertEquals("story,hindi", reloaded.getTags());
    }
}
