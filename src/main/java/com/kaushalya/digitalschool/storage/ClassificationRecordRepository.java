package com.kaushalya.digitalschool.storage;

import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClassificationRecordRepository extends JpaRepository<ClassificationRecord, UUID> {
    List<ClassificationRecord> findAllByOrderByUploadedAtDesc();
    List<ClassificationRecord> findByChatIdOrderByUploadedAtDesc(Long chatId);
    List<ClassificationRecord> findByCommunicationTypeOrderByUploadedAtDesc(CommunicationType communicationType);
    List<ClassificationRecord> findAllByFeedHiddenFalseOrderByUploadedAtDesc();
    List<ClassificationRecord> findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(Long chatId);
    List<ClassificationRecord> findByCommunicationTypeAndFeedHiddenFalseOrderByUploadedAtDesc(CommunicationType communicationType);
    List<ClassificationRecord> findByEventDateOrderByUploadedAtDesc(LocalDate eventDate);
    List<ClassificationRecord> findByFeedHiddenFalse();
    List<ClassificationRecord> findByFeedHiddenFalseAndEventDateOrderByUploadedAtDesc(LocalDate eventDate);

    @Query("SELECT r FROM ClassificationRecord r " +
            "WHERE r.eventDate BETWEEN :from AND :to " +
            "AND r.feedHidden = false " +
            "AND (:types IS NULL OR r.communicationType IN :types) " +
            "AND (:chatId IS NULL OR r.chatId = :chatId) " +
            "AND (:schoolId IS NULL OR r.chatId IN " +
            "     (SELECT u.telegramChatId FROM AppUser u WHERE u.school.id = :schoolId)) " +
            "AND (:q IS NULL OR LOWER(r.originalText) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(r.aiDescription) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "     OR LOWER(r.tags) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "AND (:tag IS NULL OR LOWER(CONCAT(',', r.tags, ',')) LIKE LOWER(CONCAT('%,', :tag, ',%'))) " +
            "ORDER BY r.eventDate DESC, r.uploadedAt DESC")
    List<ClassificationRecord> findFeed(@Param("from") LocalDate from,
                                        @Param("to") LocalDate to,
                                        @Param("types") List<CommunicationType> types,
                                        @Param("chatId") Long chatId,
                                        @Param("schoolId") UUID schoolId,
                                        @Param("q") String q,
                                        @Param("tag") String tag);

    @Query("SELECT new com.kaushalya.digitalschool.storage.TagBackfillCandidate(r.id, r.originalText, r.aiDescription) " +
            "FROM ClassificationRecord r " +
            "WHERE r.tags IS NULL OR TRIM(r.tags) = ''")
    List<TagBackfillCandidate> findTagBackfillCandidates();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ClassificationRecord r SET r.tags = :tags WHERE r.id = :id")
    int updateTags(@Param("id") UUID id, @Param("tags") String tags);

    @Query("SELECT COUNT(r) FROM ClassificationRecord r WHERE r.contentType IS NULL")
    long countMissingContentTypes();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ClassificationRecord r " +
            "SET r.contentType = CASE WHEN r.imageData IS NOT NULL THEN :imageType ELSE :textType END " +
            "WHERE r.contentType IS NULL")
    int backfillMissingContentTypes(@Param("imageType") ContentType imageType,
                                    @Param("textType") ContentType textType);
}
