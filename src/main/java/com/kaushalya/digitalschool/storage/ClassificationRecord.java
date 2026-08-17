package com.kaushalya.digitalschool.storage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "classification_record")
public class ClassificationRecord {

    @Id
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Enumerated(EnumType.STRING)
    @Column(name = "communication_type", nullable = false, length = 50)
    private CommunicationType communicationType;

    @Column(name = "image_description", columnDefinition = "TEXT")
    private String aiDescription;

    @Column(name = "image_content_type", length = 50)
    private String imageContentType;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", length = 20)
    private ContentType contentType;

    @JsonIgnore
    @Column(name = "image_data", columnDefinition = "bytea")
    private byte[] imageData;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "feed_hidden", nullable = false, columnDefinition = "boolean not null default false")
    private boolean feedHidden;

    public ClassificationRecord() {
    }

    public ClassificationRecord(UUID id, Long chatId, String originalText,
                                CommunicationType communicationType, String aiDescription,
                                LocalDate eventDate, Instant uploadedAt) {
        this(id, chatId, originalText, communicationType, aiDescription, eventDate, uploadedAt, null, null);
    }

    public ClassificationRecord(UUID id, Long chatId, String originalText,
                                CommunicationType communicationType, String aiDescription,
                                LocalDate eventDate, Instant uploadedAt,
                                byte[] imageData, String imageContentType) {
        this(id, chatId, originalText, communicationType, aiDescription, eventDate, uploadedAt,
                imageData, imageContentType, null);
    }

    public ClassificationRecord(UUID id, Long chatId, String originalText,
                                CommunicationType communicationType, String aiDescription,
                                LocalDate eventDate, Instant uploadedAt,
                                byte[] imageData, String imageContentType, String tags) {
        this.id = id;
        this.chatId = chatId;
        this.originalText = originalText;
        this.communicationType = communicationType;
        this.aiDescription = aiDescription;
        this.eventDate = eventDate;
        this.uploadedAt = uploadedAt;
        this.imageData = imageData;
        this.imageContentType = imageContentType;
        this.tags = tags;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public CommunicationType getCommunicationType() {
        return communicationType;
    }

    public void setCommunicationType(CommunicationType communicationType) {
        this.communicationType = communicationType;
    }

    public String getAiDescription() {
        return aiDescription;
    }

    public void setAiDescription(String aiDescription) {
        this.aiDescription = aiDescription;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public void setImageContentType(String imageContentType) {
        this.imageContentType = imageContentType;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public List<String> tagList() {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @JsonIgnore
    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean isFeedHidden() {
        return feedHidden;
    }

    public void setFeedHidden(boolean feedHidden) {
        this.feedHidden = feedHidden;
    }
}
