package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.storage.ExtractionRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:rulesscreen;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ExtractionRulePageRenderTest {

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
    private AiChatService aiChatService;

    @Autowired
    private ExtractionRuleRepository ruleRepository;

    @BeforeEach
    void cleanRules() {
        ruleRepository.deleteAll();
    }

    @Test
    void pageRendersAllFiveFieldsWithDefaultsAndPromptPreview() throws Exception {
        mockMvc.perform(get("/extraction-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Extraction Rules")))
                .andExpect(content().string(containsString("Classification")))
                .andExpect(content().string(containsString("Lesson date")))
                .andExpect(content().string(containsString("Scheduled events")))
                .andExpect(content().string(containsString("Tags")))
                .andExpect(content().string(containsString("Image description")))
                .andExpect(content().string(containsString("red bottom date")))
                .andExpect(content().string(containsString("YYYY-MM-DD")))
                .andExpect(content().string(containsString("Full prompt")));
    }

    @Test
    void railButtonOpensModalWithAllFieldPrompts() throws Exception {
        mockMvc.perform(get("/extraction-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("View Full prompt")))
                .andExpect(content().string(containsString("Full prompt — all fields")))
                .andExpect(content().string(containsString("Copy all")))
                .andExpect(content().string(containsString("er-modal-single")))
                .andExpect(content().string(containsString("=== Classification ===")))
                .andExpect(content().string(containsString("=== Lesson date ===")))
                .andExpect(content().string(containsString("school communication classifier")))
                .andExpect(content().string(containsString("school content tagger")));
    }

    @Test
    void modalReflectsSavedRules() throws Exception {
        mockMvc.perform(post("/extraction-rules/date")
                        .param("rules", "CUSTOM MODAL RULE TEXT")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/extraction-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CUSTOM MODAL RULE TEXT")));
    }

    @Test
    void savingRulesPersistsAndAppearsWithoutRestart() throws Exception {
        mockMvc.perform(post("/extraction-rules/date")
                        .param("rules", "MY CUSTOM DATE RULE")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/extraction-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MY CUSTOM DATE RULE")));
    }

    @Test
    void resetRestoresDefaultRules() throws Exception {
        mockMvc.perform(post("/extraction-rules/date")
                        .param("rules", "MY CUSTOM DATE RULE")
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/extraction-rules/date/reset"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/extraction-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("MY CUSTOM DATE RULE"))))
                .andExpect(content().string(containsString("red bottom date")));
    }

    @Test
    void disablingFieldShowsDisabledState() throws Exception {
        mockMvc.perform(post("/extraction-rules/tags"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/extraction-rules"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("disabled")));
    }

    @Test
    void testBoxRunsExtractionAndShowsResult() throws Exception {
        when(aiChatService.extractDate(anyString())).thenReturn("2026-08-01");

        mockMvc.perform(multipart("/extraction-rules/date/test")
                        .param("sampleText", "notebook page"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2026-08-01")));
    }
}
