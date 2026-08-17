package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeedLinksTest {

    private final FeedLinks links = new FeedLinks();

    @Test
    void tagUrlOmitsNullFilters() {
        assertEquals("/feed?tag=story", links.tagUrl("story", null, null, null, null));
    }

    @Test
    void tagUrlComposesWithSearchAndFilters() {
        assertEquals("/feed?tag=story&q=lesson&types=REMINDER&types=EVENT&chatId=42",
                links.tagUrl("story", "lesson", List.of(CommunicationType.REMINDER, CommunicationType.EVENT), 42L, null));
    }

    @Test
    void tagUrlEncodesValues() {
        assertEquals("/feed?tag=science%20fair&q=a%20b",
                links.tagUrl("science fair", "a b", null, null, null));
    }

    @Test
    void tagUrlKeepsSchool() {
        UUID schoolId = UUID.randomUUID();

        assertEquals("/feed?tag=story&schoolId=" + schoolId,
                links.tagUrl("story", null, null, null, schoolId));
    }

    @Test
    void removeTagUrlKeepsSearchAndFilters() {
        assertEquals("/feed?q=lesson&types=REMINDER&chatId=42",
                links.removeTagUrl("lesson", List.of(CommunicationType.REMINDER), 42L, null));
    }

    @Test
    void removeTagUrlKeepsSchool() {
        UUID schoolId = UUID.randomUUID();

        assertEquals("/feed?q=lesson&schoolId=" + schoolId,
                links.removeTagUrl("lesson", null, null, schoolId));
    }

    @Test
    void clearSearchUrlKeepsFiltersButDropsSearchTerm() {
        assertEquals("/feed?types=REMINDER&chatId=42&tag=story",
                links.clearSearchUrl(List.of(CommunicationType.REMINDER), 42L, "story", null));
    }

    @Test
    void clearSearchUrlKeepsSchool() {
        UUID schoolId = UUID.randomUUID();

        assertEquals("/feed?chatId=42&tag=story&schoolId=" + schoolId,
                links.clearSearchUrl(null, 42L, "story", schoolId));
    }
}
