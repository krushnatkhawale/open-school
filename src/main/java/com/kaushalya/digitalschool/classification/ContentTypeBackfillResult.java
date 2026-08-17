package com.kaushalya.digitalschool.classification;

public record ContentTypeBackfillResult(long candidates, long updated, long failed,
                                        boolean alreadyRunning) {

    public static ContentTypeBackfillResult inProgress() {
        return new ContentTypeBackfillResult(0, 0, 0, true);
    }
}
