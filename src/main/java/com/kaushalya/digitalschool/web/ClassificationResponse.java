package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClassificationResponse(
        UUID id,
        Long chatId,
        String originalText,
        CommunicationType communicationType,
        String aiDescription,
        String imageContentType,
        String tags,
        ContentType contentType,
        LocalDate eventDate,
        Instant uploadedAt,
        boolean feedHidden) {

    public static ClassificationResponse from(ClassificationRecord record) {
        return new ClassificationResponse(
                record.getId(),
                record.getChatId(),
                record.getOriginalText(),
                record.getCommunicationType(),
                record.getAiDescription(),
                record.getImageContentType(),
                record.getTags(),
                record.getContentType(),
                record.getEventDate(),
                record.getUploadedAt(),
                record.isFeedHidden());
    }
}
