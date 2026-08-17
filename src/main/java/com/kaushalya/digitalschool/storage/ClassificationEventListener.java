package com.kaushalya.digitalschool.storage;

import com.kaushalya.digitalschool.shared.ClassificationEvent;
import com.kaushalya.digitalschool.shared.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
public class ClassificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ClassificationEventListener.class);

    private final ClassificationRecordRepository repository;

    public ClassificationEventListener(ClassificationRecordRepository repository) {
        this.repository = repository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(ClassificationEvent event) {
        log.info(">> ENTER ClassificationEventListener.on() for chatId={} type={}", event.chatId(), event.type());
        try {
            LocalDate eventDate = event.eventDate() != null ? event.eventDate() : event.uploadedAt().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            var record = new ClassificationRecord(
                    UUID.randomUUID(),
                    event.chatId(),
                    event.originalText(),
                    event.type(),
                    event.aiDescription(),
                    eventDate,
                    event.uploadedAt(),
                    event.imageData(),
                    event.imageContentType(),
                    joinTags(event.tags())
            );
            record.setContentType(event.imageData() != null ? ContentType.IMAGE : ContentType.TEXT);
            record.setFeedHidden(event.feedHidden());
            log.info("Saving record: id={} chatId={} type={} eventDate={} feedHidden={}", record.getId(), record.getChatId(), record.getCommunicationType(), record.getEventDate(), record.isFeedHidden());
            repository.save(record);
            log.info("<< EXIT ClassificationEventListener.on() — save completed successfully for {}", record.getId());
        } catch (Exception e) {
            log.error("!! FAILED to persist classification event: chatId={} type={} error={}", event.chatId(), event.type(), e.getMessage(), e);
        }
    }

    private static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return String.join(",", tags);
    }
}
