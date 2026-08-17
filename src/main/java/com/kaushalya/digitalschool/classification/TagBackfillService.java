package com.kaushalya.digitalschool.classification;

import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import com.kaushalya.digitalschool.storage.TagBackfillCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TagBackfillService {

    private static final Logger log = LoggerFactory.getLogger(TagBackfillService.class);

    private final ClassificationRecordRepository repository;
    private final AiChatService aiChatService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public TagBackfillService(ClassificationRecordRepository repository, AiChatService aiChatService) {
        this.repository = repository;
        this.aiChatService = aiChatService;
    }

    @Transactional
    public TagBackfillResult run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Tag backfill already in progress; skipping duplicate run");
            return TagBackfillResult.inProgress();
        }
        try {
            List<TagBackfillCandidate> candidates = repository.findTagBackfillCandidates();
            log.info("Tag backfill starting: {} candidates missing tags", candidates.size());

            int tagged = 0;
            int noTags = 0;
            int skipped = 0;
            int failed = 0;

            for (TagBackfillCandidate candidate : candidates) {
                String text = textToTag(candidate.originalText(), candidate.aiDescription());
                if (text == null) {
                    skipped++;
                    log.debug("Skipping record {}: no text to tag", candidate.id());
                    continue;
                }
                try {
                    List<String> tags = aiChatService.extractTags(text);
                    if (tags.isEmpty()) {
                        noTags++;
                        log.info("No tags extracted for record {}", candidate.id());
                        continue;
                    }
                    repository.updateTags(candidate.id(), String.join(",", tags));
                    tagged++;
                } catch (Exception e) {
                    failed++;
                    log.warn("Tag extraction failed for record {}: {}", candidate.id(), e.getMessage());
                }
            }

            TagBackfillResult result = new TagBackfillResult(candidates.size(), tagged, noTags, skipped, failed, false);
            log.info("Tag backfill complete: candidates={} tagged={} noTags={} skipped={} failed={}",
                    result.candidates(), result.tagged(), result.noTags(), result.skipped(), result.failed());
            return result;
        } finally {
            running.set(false);
        }
    }

    static String textToTag(String originalText, String aiDescription) {
        boolean hasText = originalText != null && !originalText.isBlank();
        boolean hasDescription = aiDescription != null && !aiDescription.isBlank();
        if (!hasText && !hasDescription) {
            return null;
        }
        if (!hasText) {
            return aiDescription;
        }
        if (!hasDescription) {
            return originalText;
        }
        return originalText + "\n" + aiDescription;
    }
}
