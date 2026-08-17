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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:classrender;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ClassificationPageRenderTest {

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

    private String visibleText = "Homework for tomorrow";
    private String hiddenText = "What is the name of the school you represent?";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        Instant now = Instant.now();

        ClassificationRecord visible = new ClassificationRecord(UUID.randomUUID(), 12345L,
                visibleText, CommunicationType.DAILY_LESSON_UPDATE, null,
                LocalDate.now(), now);
        visible.setContentType(ContentType.TEXT);
        repository.save(visible);

        ClassificationRecord hidden = new ClassificationRecord(UUID.randomUUID(), 12345L,
                hiddenText, CommunicationType.GENERAL_INFORMATION, null,
                LocalDate.now(), now.minusSeconds(1));
        hidden.setContentType(ContentType.TEXT);
        hidden.setFeedHidden(true);
        repository.save(hidden);
    }

    @Test
    void listRendersVisibleRecordsButNotFeedHiddenOnes() throws Exception {
        mockMvc.perform(get("/classifications"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(visibleText)))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(hiddenText))));
    }
}
