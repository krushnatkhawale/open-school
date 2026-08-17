package com.kaushalya.digitalschool;

import com.kaushalya.digitalschool.classification.AiChatService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MyPrivateDigitalSchoolApplicationTests {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            return Mockito.mock(AiChatService.class);
        }
    }

    @Test
    void contextLoads() {
    }
}
