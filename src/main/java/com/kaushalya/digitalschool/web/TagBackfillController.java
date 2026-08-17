package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.TagBackfillResult;
import com.kaushalya.digitalschool.classification.TagBackfillService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backfill-tags")
@ConditionalOnProperty(name = "app.tag-backfill.enabled", havingValue = "true")
public class TagBackfillController {

    private final TagBackfillService tagBackfillService;

    public TagBackfillController(TagBackfillService tagBackfillService) {
        this.tagBackfillService = tagBackfillService;
    }

    @PostMapping
    public TagBackfillResult run() {
        return tagBackfillService.run();
    }
}
