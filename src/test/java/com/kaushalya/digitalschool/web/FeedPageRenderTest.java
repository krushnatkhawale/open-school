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
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:feedrender;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class FeedPageRenderTest {

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

    private UUID imageRecordId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();

        ClassificationRecord imageRecord = new ClassificationRecord(UUID.randomUUID(), 12345L,
                "Sunrise over the lake",
                CommunicationType.REMINDER, "Sunrise over the lake",
                today, now);
        imageRecord.setImageData(new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        imageRecord.setImageContentType("image/png");
        imageRecord.setContentType(ContentType.IMAGE);
        imageRecordId = imageRecord.getId();
        repository.save(imageRecord);

        ClassificationRecord textRecord = new ClassificationRecord(UUID.randomUUID(), 12345L,
                "Homework for tomorrow",
                CommunicationType.DAILY_LESSON_UPDATE, null,
                today, now.minusSeconds(1));
        textRecord.setContentType(ContentType.TEXT);
        repository.save(textRecord);
    }

    @Test
    void imageRecordRendersSingleTextNextToImageInMediaGrid() throws Exception {
        mockMvc.perform(get("/feed"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("feed-card-media")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/classifications/" + imageRecordId + "/image")));

        String html = mockMvc.perform(get("/feed")).andReturn().getResponse().getContentAsString();

        assertEquals(1, StringUtils.countOccurrencesOf(html, "class=\"feed-card-media\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "class=\"feed-card-media-image\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "class=\"feed-card-media-text\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "class=\"feed-card-text\""));
        assertEquals(2, StringUtils.countOccurrencesOf(html, "class=\"feed-card-text-wrap\""));
        assertEquals(2, StringUtils.countOccurrencesOf(html, "class=\"feed-card-more\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html,
                "<p class=\"feed-card-media-text\">Sunrise over the lake</p>"));
        assertEquals(1, StringUtils.countOccurrencesOf(html,
                "<p class=\"feed-card-text\">Homework for tomorrow</p>"));
    }

    @Test
    void textOnlyFeedRendersFullWidthTextWithoutAnyMediaGrid() throws Exception {
        repository.deleteAll();
        ClassificationRecord textRecord = new ClassificationRecord(UUID.randomUUID(), 12345L,
                "Homework for tomorrow",
                CommunicationType.DAILY_LESSON_UPDATE, null,
                LocalDate.now(), Instant.now());
        textRecord.setContentType(ContentType.TEXT);
        repository.save(textRecord);

        String html = mockMvc.perform(get("/feed")).andReturn().getResponse().getContentAsString();

        assertEquals(1, StringUtils.countOccurrencesOf(html, "class=\"feed-card-text\""));
        assertEquals(0, StringUtils.countOccurrencesOf(html, "class=\"feed-card-media\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "class=\"feed-card-text-wrap\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "class=\"feed-card-more\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html,
                "<p class=\"feed-card-text\">Homework for tomorrow</p>"));
    }

    @Test
    void searchHighlightStillAppliesToImageCardText() throws Exception {
        String html = mockMvc.perform(get("/feed").param("q", "sunrise"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, StringUtils.countOccurrencesOf(html,
                "<p class=\"feed-card-media-text\"><mark>Sunrise</mark> over the lake</p>"));
    }

    @Test
    void schoolDropdownRendersWithAllSchoolsOption() throws Exception {
        String html = mockMvc.perform(get("/feed"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, StringUtils.countOccurrencesOf(html, "id=\"schoolSelect\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "value=\"\">All schools"));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "aria-label=\"Select school\""));
    }

    @Test
    void navbarJavascriptIsRenderedInPage() throws Exception {
        String html = mockMvc.perform(get("/feed"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(1, StringUtils.countOccurrencesOf(html, "getElementById('navToggle')"));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "getElementById('schoolSelect')"));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "window.location.href"));
    }

    @Test
    void themeSwitcherAndAnimatedBackgroundScriptsAreRendered() throws Exception {
        String html = mockMvc.perform(get("/feed"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(2, StringUtils.countOccurrencesOf(html, "class=\"theme-btn"));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "data-theme=\"static\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "data-theme=\"animated\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "id=\"three-bg\""));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "theme-animated.js"));
        assertEquals(1, StringUtils.countOccurrencesOf(html, "type=\"importmap\""));
    }
}
