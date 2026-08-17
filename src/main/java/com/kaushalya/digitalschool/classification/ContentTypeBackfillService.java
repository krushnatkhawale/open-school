package com.kaushalya.digitalschool.classification;

import com.kaushalya.digitalschool.shared.ContentType;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ContentTypeBackfillService {

    private static final Logger log = LoggerFactory.getLogger(ContentTypeBackfillService.class);

    private final ClassificationRecordRepository repository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ContentTypeBackfillService(ClassificationRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ContentTypeBackfillResult run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Content type backfill already in progress; skipping duplicate run");
            return ContentTypeBackfillResult.inProgress();
        }
        try {
            long candidates = repository.countMissingContentTypes();
            if (candidates == 0) {
                log.info("Content type backfill: no records missing a content type");
                return new ContentTypeBackfillResult(0, 0, 0, false);
            }
            int updated = repository.backfillMissingContentTypes(ContentType.IMAGE, ContentType.TEXT);
            ContentTypeBackfillResult result = new ContentTypeBackfillResult(
                    candidates, updated, candidates - updated, false);
            log.info("Content type backfill complete: candidates={} updated={} failed={}",
                    result.candidates(), result.updated(), result.failed());
            return result;
        } finally {
            running.set(false);
        }
    }
}
