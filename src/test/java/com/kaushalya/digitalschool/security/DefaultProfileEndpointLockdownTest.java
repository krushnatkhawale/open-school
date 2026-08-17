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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:lockdown;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class DefaultProfileEndpointLockdownTest {

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
    void sensitiveActuatorEndpointsAreNotExposedOnDefaultProfile() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/configprops")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/loggers")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/mappings")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/conditions")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/startup")).andExpect(status().isNotFound());
        mockMvc.perform(get("/actuator/scheduledtasks")).andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointStaysUpWithoutDetailsOnDefaultProfile() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void h2ConsoleIsDisabledOnDefaultProfile() {
        assertThat(environment.getProperty("spring.h2.console.enabled")).isEqualTo("false");
    }

    @Test
    void bootUiApiIsDisabledOnDefaultProfile() throws Exception {
        mockMvc.perform(get("/bootui/api/health")).andExpect(status().isNotFound());
    }
}
