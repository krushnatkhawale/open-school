package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.ContentTypeBackfillResult;
import com.kaushalya.digitalschool.classification.ContentTypeBackfillService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backfill-content-type")
@ConditionalOnProperty(name = "app.content-type-backfill.enabled", havingValue = "true")
public class ContentTypeBackfillController {

    private final ContentTypeBackfillService contentTypeBackfillService;

    public ContentTypeBackfillController(ContentTypeBackfillService contentTypeBackfillService) {
        this.contentTypeBackfillService = contentTypeBackfillService;
    }

    @PostMapping
    public ContentTypeBackfillResult run() {
        return contentTypeBackfillService.run();
    }
}
