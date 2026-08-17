package com.kaushalya.digitalschool.classification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ScheduledEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.springframework.ai.ollama.api.OllamaChatOptions.builder;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final ChatClient chatClient;
    private final String visionModel;
    private final ExtractionRuleService extractionRuleService;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             @Value("${ollama.vision-model}") String visionModel,
                             ExtractionRuleService extractionRuleService) {
        this.chatClient = chatClientBuilder.build();
        this.visionModel = visionModel;
        this.extractionRuleService = extractionRuleService;
        log.info("AI chat client initialized (vision model: {})", visionModel);
    }

    @Override
    public String chat(String message, String userId) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @Override
    public CommunicationType classify(String message) {
        return classify(message, null, null);
    }

    @Override
    public String describeImage(byte[] imageData, String mediaType) {
        if (imageData == null || mediaType == null) {
            log.warn("describeImage called with null image data or media type");
            return "No image data available.";
        }

        String response = chatClient.prompt()
                .system(extractionRuleService.systemPromptFor(ExtractionField.IMAGE_DESCRIPTION))
                .user(u -> u.text(ExtractionField.IMAGE_DESCRIPTION.userInstruction())
                        .media(MimeType.valueOf(mediaType),
                                new InputStreamResource(new ByteArrayInputStream(imageData))))
                .options(builder().model(visionModel))
                .call()
                .content();

        log.debug("Image description: '{}'", response);
        return response != null ? response.trim() : "Could not describe the image.";
    }

    @Override
    public String extractDate(String text) {
        if (text == null || text.isBlank()) return null;

        try {
            String response = chatClient.prompt()
                    .system(extractionRuleService.systemPromptFor(ExtractionField.DATE))
                    .user(text)
                    .call()
                    .content();

            String trimmed = response != null ? response.trim() : "NONE";
            log.info("Extracted date: '{}' from text='{}'", trimmed, text.substring(0, Math.min(text.length(), 100)));

            if ("NONE".equalsIgnoreCase(trimmed)) return null;

            LocalDate parsed = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
            if (parsed.getYear() < 2000) {
                parsed = parsed.plusYears(100);
            }
            return parsed.toString();
        } catch (Exception e) {
            log.warn("Date extraction failed: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<ScheduledEventData> extractScheduledEvents(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        try {
            String response = chatClient.prompt()
                    .system(extractionRuleService.systemPromptFor(ExtractionField.SCHEDULED_EVENTS))
                    .user(text)
                    .call()
                    .content();

            if (response == null || response.isBlank()) return Collections.emptyList();

            String trimmed = response.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                ObjectMapper mapper = new ObjectMapper();
                List<ScheduledEventData> events = mapper.readValue(trimmed,
                        new TypeReference<List<ScheduledEventData>>() {});
                log.info("Extracted {} scheduled events from text", events.size());
                return events;
            }
        } catch (Exception e) {
            log.warn("Scheduled event extraction failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> extractTags(String text) {
        if (text == null || text.isBlank()) return Collections.emptyList();

        try {
            String response = chatClient.prompt()
                    .system(extractionRuleService.systemPromptFor(ExtractionField.TAGS))
                    .user(text)
                    .call()
                    .content();

            String trimmed = response != null ? response.trim() : "NONE";
            log.info("Extracted tags: '{}' from text='{}'", trimmed, text.substring(0, Math.min(text.length(), 100)));

            if ("NONE".equalsIgnoreCase(trimmed)) return Collections.emptyList();

            return Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .map(t -> t.toLowerCase(Locale.ROOT))
                    .filter(t -> !t.isEmpty())
                    .distinct()
                    .toList();
        } catch (Exception e) {
            log.warn("Tag extraction failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public CommunicationType classify(String message, byte[] imageData, String mediaType) {
        String response;
        boolean hasImage = imageData != null && mediaType != null;

        log.info("Classifying text='{}' hasImage={}", message != null ? message.substring(0, Math.min(message.length(), 200)) : null, hasImage);

        if (hasImage) {
            response = chatClient.prompt()
                    .system(extractionRuleService.systemPromptFor(ExtractionField.CLASSIFICATION))
                    .user(u -> {
                        if (message != null && !message.isBlank()) {
                            u.text(message);
                        }
                        u.media(MimeType.valueOf(mediaType),
                                new InputStreamResource(new ByteArrayInputStream(imageData)));
                    })
                    .options(builder().model(visionModel))
                    .call()
                    .content();
        } else {
            if (message == null || message.isBlank()) {
                log.warn("classify called with null/blank text and no image");
                return CommunicationType.GENERAL_INFORMATION;
            }
            response = chatClient.prompt()
                    .system(extractionRuleService.systemPromptFor(ExtractionField.CLASSIFICATION))
                    .user(message)
                    .call()
                    .content();
        }

        String trimmed = response != null ? response.trim() : "";
        log.info("Classification response: '{}'", trimmed);

        if (!trimmed.isEmpty()) {
            var exactMatch = findExact(trimmed);
            if (exactMatch.isPresent()) {
                log.info("Classified as: {}", exactMatch.get());
                return exactMatch.get();
            }

            var fuzzyMatch = findFuzzy(trimmed);
            if (fuzzyMatch.isPresent()) {
                log.warn("Fuzzy matched '{}' to '{}'", trimmed, fuzzyMatch.get());
                return fuzzyMatch.get();
            }
        }

        log.warn("Unrecognized classification '{}', falling back to GENERAL_INFORMATION", trimmed);
        return CommunicationType.GENERAL_INFORMATION;
    }

    private static Optional<CommunicationType> findExact(String value) {
        for (var type : CommunicationType.values()) {
            if (type.name().equals(value)) return Optional.of(type);
        }
        return Optional.empty();
    }

    private static Optional<CommunicationType> findFuzzy(String value) {
        String upper = value.toUpperCase();
        CommunicationType best = null;
        int bestDist = Integer.MAX_VALUE;

        for (var type : CommunicationType.values()) {
            int dist = levenshtein(upper, type.name());
            if (dist < bestDist) {
                bestDist = dist;
                best = type;
            }
        }

        return bestDist <= 3 ? Optional.of(best) : Optional.empty();
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[a.length()][b.length()];
    }
}
