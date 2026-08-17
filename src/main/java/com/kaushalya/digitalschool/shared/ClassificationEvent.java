package com.kaushalya.digitalschool.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ClassificationEvent(
    Long chatId,
    String originalText,
    CommunicationType type,
    String aiDescription,
    LocalDate eventDate,
    Instant uploadedAt,
    byte[] imageData,
    String imageContentType,
    List<String> tags,
    boolean feedHidden
) {
    public ClassificationEvent(Long chatId, String originalText, CommunicationType type, String aiDescription) {
        this(chatId, originalText, type, aiDescription, null, Instant.now(), null, null, List.of(), false);
    }

    public ClassificationEvent(Long chatId, String originalText, CommunicationType type, String aiDescription,
                               LocalDate eventDate, Instant uploadedAt) {
        this(chatId, originalText, type, aiDescription, eventDate, uploadedAt, null, null, List.of(), false);
    }

    public ClassificationEvent(Long chatId, String originalText, CommunicationType type, String aiDescription,
                               LocalDate eventDate, Instant uploadedAt, byte[] imageData, String imageContentType) {
        this(chatId, originalText, type, aiDescription, eventDate, uploadedAt, imageData, imageContentType, List.of(), false);
    }
}
