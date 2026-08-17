package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:apicontract;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ClassificationApiContractTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            return Mockito.mock(AiChatService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClassificationRecordRepository repository;

    private UUID id;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        id = UUID.randomUUID();
        ClassificationRecord record = new ClassificationRecord(id, 12345L, "Algebra homework",
                CommunicationType.DAILY_LESSON_UPDATE, "Lesson on quadratic equations",
                LocalDate.of(2026, 8, 1), Instant.parse("2026-08-01T09:00:00Z"),
                new byte[]{1, 2, 3}, "image/png");
        record.setContentType(ContentType.IMAGE);
        record.setTags("maths");
        repository.save(record);
    }

    @Test
    void getAllReturnsDtosWithoutJpaImageData() throws Exception {
        mockMvc.perform(get("/api/classifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].chatId").value(12345))
                .andExpect(jsonPath("$[0].originalText").value("Algebra homework"))
                .andExpect(jsonPath("$[0].communicationType").value("DAILY_LESSON_UPDATE"))
                .andExpect(jsonPath("$[0].aiDescription").value("Lesson on quadratic equations"))
                .andExpect(jsonPath("$[0].imageContentType").value("image/png"))
                .andExpect(jsonPath("$[0].tags").value("maths"))
                .andExpect(jsonPath("$[0].contentType").value("IMAGE"))
                .andExpect(jsonPath("$[0].eventDate").value("2026-08-01"))
                .andExpect(jsonPath("$[0].uploadedAt").value("2026-08-01T09:00:00Z"))
                .andExpect(jsonPath("$[0].feedHidden").value(false))
                .andExpect(jsonPath("$[0].imageData").doesNotExist());
    }

    @Test
    void updateRejectsMissingCommunicationType() throws Exception {
        mockMvc.perform(put("/api/classifications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventDate\":\"2026-08-10\",\"originalText\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("communicationType"));
    }

    @Test
    void updateRejectsMissingEventDate() throws Exception {
        mockMvc.perform(put("/api/classifications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"communicationType\":\"EVENT\",\"originalText\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("eventDate"));
    }

    @Test
    void updateRejectsMalformedBody() throws Exception {
        mockMvc.perform(put("/api/classifications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"communicationType\":\"NOT_A_TYPE\",\"eventDate\":\"2026-08-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed"));
    }

    @Test
    void updatePersistsAndReturnsDto() throws Exception {
        mockMvc.perform(put("/api/classifications/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"communicationType\":\"REMINDER\",\"eventDate\":\"2026-08-10\",\"originalText\":\"Exam on Friday\",\"tags\":\"exam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.communicationType").value("REMINDER"))
                .andExpect(jsonPath("$.originalText").value("Exam on Friday"))
                .andExpect(jsonPath("$.eventDate").value("2026-08-10"))
                .andExpect(jsonPath("$.tags").value("exam"))
                .andExpect(jsonPath("$.imageData").doesNotExist());
    }

    @Test
    void updateReturns404ForUnknownId() throws Exception {
        mockMvc.perform(put("/api/classifications/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"communicationType\":\"REMINDER\",\"eventDate\":\"2026-08-10\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByIdReturnsDto() throws Exception {
        mockMvc.perform(get("/api/classifications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.originalText").value(containsString("Algebra")))
                .andExpect(jsonPath("$.imageData").doesNotExist());
    }

    @Test
    void getByIdReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/classifications/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
