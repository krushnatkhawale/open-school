package com.kaushalya.digitalschool.storage;

import java.util.UUID;

public record TagBackfillCandidate(UUID id, String originalText, String aiDescription) {
}
