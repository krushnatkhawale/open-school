package com.kaushalya.digitalschool.security;

import com.kaushalya.digitalschool.classification.AiChatService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:local;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class LocalProfileEndpointExposureTest {

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
    private Environment environment;

    @Test
    void sensitiveActuatorEndpointsAreExposedOnLocalProfile() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/configprops")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/loggers")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/mappings")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/conditions")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/startup")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/scheduledtasks")).andExpect(status().isOk());
    }

    @Test
    void healthEndpointShowsDetailsOnLocalProfile() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").exists());
    }

    @Test
    void h2ConsoleIsEnabledOnLocalProfile() {
        assertThat(environment.getProperty("spring.h2.console.enabled")).isEqualTo("true");
    }

    @Test
    void bootUiApiIsEnabledOnLocalProfile() throws Exception {
        mockMvc.perform(get("/bootui/api/health")).andExpect(status().isOk());
    }
}
