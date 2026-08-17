package com.kaushalya.digitalschool.classification;

import java.time.Instant;
import java.util.List;

public record ExtractionRuleView(
        String key,
        String label,
        String description,
        String rules,
        boolean enabled,
        boolean usingDefault,
        Instant updatedAt,
        String fullPrompt) {
}
