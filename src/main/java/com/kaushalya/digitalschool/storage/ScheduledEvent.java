package com.kaushalya.digitalschool.storage;

import com.kaushalya.digitalschool.shared.ScheduledEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scheduled_event")
public class ScheduledEvent {

    @Id
    private UUID id;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ScheduledEventType eventType;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "source_text", columnDefinition = "TEXT")
    private String sourceText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ScheduledEvent() {
    }

    public ScheduledEvent(UUID id, LocalDate eventDate, String title, ScheduledEventType eventType,
                          Long chatId, String sourceText, Instant createdAt) {
        this.id = id;
        this.eventDate = eventDate;
        this.title = title;
        this.eventType = eventType;
        this.chatId = chatId;
        this.sourceText = sourceText;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public ScheduledEventType getEventType() { return eventType; }
    public void setEventType(ScheduledEventType eventType) { this.eventType = eventType; }
    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }
    public String getSourceText() { return sourceText; }
    public void setSourceText(String sourceText) { this.sourceText = sourceText; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
