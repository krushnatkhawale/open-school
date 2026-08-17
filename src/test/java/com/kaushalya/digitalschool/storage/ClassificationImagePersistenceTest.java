package com.kaushalya.digitalschool.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:imagetest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ClassificationImagePersistenceTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
    }

    @Test
    void imageBytesSurvivePersistAndReload() {
        byte[] png = sampleImageLargerThan255Bytes();
        ClassificationRecord saved = repository.save(new ClassificationRecord(
                UUID.randomUUID(), 12345L, "text", CommunicationType.REMINDER,
                "desc", LocalDate.now(), Instant.now(), png, "image/png"));

        ClassificationRecord loaded = repository.findById(saved.getId()).orElseThrow();

        assertTrue(loaded.getImageData() != null, "imageData should be present after reload");
        assertArrayEquals(png, loaded.getImageData());
        assertEquals("image/png", loaded.getImageContentType());
    }

    private byte[] sampleImageLargerThan255Bytes() {
        byte[] png = new byte[300];
        byte[] signature = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, png, 0, signature.length);
        for (int i = signature.length; i < png.length; i++) {
            png[i] = (byte) (i % 256);
        }
        return png;
    }

    @Test
    void jsonSerializationOmitsBlobButKeepsMimeType() throws Exception {
        repository.save(new ClassificationRecord(
                UUID.randomUUID(), 12345L, "text", CommunicationType.REMINDER,
                "desc", LocalDate.now(), Instant.now(),
                new byte[]{1, 2, 3}, "image/png"));

        ClassificationRecord loaded = repository.findAll().get(0);
        String json = objectMapper.writeValueAsString(loaded);

        assertTrue(json.contains("\"imageContentType\":\"image/png\""), "json should expose imageContentType: " + json);
        assertFalse(json.contains("imageData"), "json must not leak the blob: " + json);
    }
}
