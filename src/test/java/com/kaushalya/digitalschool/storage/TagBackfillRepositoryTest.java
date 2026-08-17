package com.kaushalya.digitalschool.storage;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.shared.CommunicationType;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:backfilltest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TagBackfillRepositoryTest {

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
    void findTagBackfillCandidatesReturnsOnlyRecordsMissingTags() {
        UUID missing = UUID.randomUUID();
        UUID blank = UUID.randomUUID();
        UUID tagged = UUID.randomUUID();

        repository.save(record(missing, null));
        repository.save(record(blank, "   "));
        ClassificationRecord hasTags = record(tagged, "story,hindi");
        repository.save(hasTags);

        List<TagBackfillCandidate> candidates = repository.findTagBackfillCandidates();

        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().anyMatch(c -> c.id().equals(missing)));
        assertTrue(candidates.stream().anyMatch(c -> c.id().equals(blank)));
        assertTrue(candidates.stream().noneMatch(c -> c.id().equals(tagged)));
    }

    @Test
    void findTagBackfillCandidatesCarriesTextAndDescription() {
        UUID id = UUID.randomUUID();
        ClassificationRecord record = record(id, null);
        record.setOriginalText("Story about a frog");
        record.setAiDescription("A page from a Hindi reader");
        repository.save(record);

        List<TagBackfillCandidate> candidates = repository.findTagBackfillCandidates();

        assertEquals(1, candidates.size());
        assertEquals(id, candidates.get(0).id());
        assertEquals("Story about a frog", candidates.get(0).originalText());
        assertEquals("A page from a Hindi reader", candidates.get(0).aiDescription());
    }

    @Test
    @Transactional
    void updateTagsPersistsCommaJoinedTags() {
        ClassificationRecord record = record(UUID.randomUUID(), null);
        repository.save(record);

        int updated = repository.updateTags(record.getId(), "story,hindi");

        assertEquals(1, updated);
        ClassificationRecord reloaded = repository.findById(record.getId()).orElseThrow();
        assertEquals("story,hindi", reloaded.getTags());
    }

    private ClassificationRecord record(UUID id, String tags) {
        ClassificationRecord record = new ClassificationRecord(id, 111L, "Original text",
                CommunicationType.DAILY_LESSON_UPDATE, "Image description",
                LocalDate.now(), Instant.now());
        record.setTags(tags);
        return record;
    }
}
