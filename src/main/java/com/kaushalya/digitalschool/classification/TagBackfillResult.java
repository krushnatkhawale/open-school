package com.kaushalya.digitalschool.classification;

public record TagBackfillResult(int candidates, int tagged, int noTags, int skipped, int failed,
                                boolean alreadyRunning) {

    public static TagBackfillResult inProgress() {
        return new TagBackfillResult(0, 0, 0, 0, 0, true);
    }
}
