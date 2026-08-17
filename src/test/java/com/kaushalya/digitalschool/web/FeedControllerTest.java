package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.onboarding.AppUserRepository;
import com.kaushalya.digitalschool.onboarding.SchoolRepository;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ScheduledEventType;
import com.kaushalya.digitalschool.shared.SchoolProperties;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import com.kaushalya.digitalschool.storage.ScheduledEvent;
import com.kaushalya.digitalschool.storage.ScheduledEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.support.BindingAwareModelMap;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedControllerTest {

    private ClassificationRecordRepository repository;
    private ScheduledEventRepository scheduledEventRepository;
    private SchoolRepository schoolRepository;
    private AppUserRepository appUserRepository;
    private FeedController controller;

    @BeforeEach
    void setUp() {
        repository = mock(ClassificationRecordRepository.class);
        scheduledEventRepository = mock(ScheduledEventRepository.class);
        when(scheduledEventRepository.findByEventDateBetweenOrderByEventDateAsc(any(), any()))
                .thenReturn(List.of());
        when(scheduledEventRepository.findByChatIdAndEventDateBetweenOrderByEventDateAsc(any(), any(), any()))
                .thenReturn(List.of());
        schoolRepository = mock(SchoolRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        when(schoolRepository.findAllByOrderByNameAsc()).thenReturn(List.of());
        SchoolProperties properties = new SchoolProperties(4);
        controller = new FeedController(repository, scheduledEventRepository, properties,
                new FeedSearch(), new FeedLinks(), schoolRepository, appUserRepository);
    }

    @Test
    void defaultsToWholeAcademicYearNewestFirst() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, null))
                .thenReturn(List.of(record(now.atDay(5))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        String view = controller.feed(null, null, null, null, null, model);

        assertEquals("feed", view);
        List<FeedController.MonthSection> sections = sectionsOf(model);
        long months = now.until(academicStart, java.time.temporal.ChronoUnit.MONTHS);
        assertEquals(Math.abs(months) + 1, sections.size());
        assertEquals(now, YearMonth.of(sections.get(0).year(), sections.get(0).month()));
        assertEquals(academicStart, YearMonth.of(sections.get(sections.size() - 1).year(), sections.get(sections.size() - 1).month()));
        assertEquals(1, sections.get(0).records().size());
        assertEquals(Set.of(5), sections.get(0).daysWithPosts());
        assertEquals(1, model.get("totalCount"));
        assertNull(model.get("selectedTypes"));
        assertNull(model.get("selectedChatId"));
        assertNull(model.get("selectedSchoolId"));
        assertNull(model.get("searchTerm"));
        assertNull(model.get("searchNote"));
    }

    @Test
    void groupsRecordsIntoTheirMonthsAndKeepsEmptyMonths() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        LocalDate twoMonthsAgo = now.minusMonths(2).atDay(15);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, null))
                .thenReturn(List.of(record(now.atDay(2)), record(twoMonthsAgo)));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, null, null, null, model);

        List<FeedController.MonthSection> sections = sectionsOf(model);
        assertEquals(now, YearMonth.of(sections.get(0).year(), sections.get(0).month()));
        assertEquals(1, sections.get(0).records().size());
        assertEquals(now.minusMonths(1), YearMonth.of(sections.get(1).year(), sections.get(1).month()));
        assertTrue(sections.get(1).records().isEmpty());
        assertTrue(sections.get(1).daysWithPosts().isEmpty());
        assertEquals(now.minusMonths(2), YearMonth.of(sections.get(2).year(), sections.get(2).month()));
        assertEquals(1, sections.get(2).records().size());
        assertEquals(Set.of(15), sections.get(2).daysWithPosts());
        assertEquals(2, model.get("totalCount"));
    }

    @Test
    void passesFiltersThrough() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        List<CommunicationType> types = List.of(CommunicationType.EVENT, CommunicationType.REMINDER);
        Long chat = 42L;
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), types, chat, null, null, null))
                .thenReturn(List.of(record(now.atDay(3))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        String view = controller.feed(types, chat, null, null, null, model);

        assertEquals("feed", view);
        assertEquals(types, model.get("selectedTypes"));
        assertEquals(chat, model.get("selectedChatId"));
        assertEquals(1, model.get("totalCount"));
    }

    @Test
    void passesSearchTermThrough() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, "exam", null))
                .thenReturn(List.of(record(now.atDay(3))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        String view = controller.feed(null, null, null, "exam", null, model);

        assertEquals("feed", view);
        assertEquals("exam", model.get("searchTerm"));
        assertEquals("1 result for \"exam\"", model.get("searchNote"));
        assertEquals(1, model.get("totalCount"));
    }

    @Test
    void passesTagFilterThrough() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, "story"))
                .thenReturn(List.of(record(now.atDay(3))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        String view = controller.feed(null, null, null, null, "story", model);

        assertEquals("feed", view);
        assertEquals("story", model.get("selectedTag"));
        assertEquals(1, model.get("totalCount"));
    }

    @Test
    void tagFilterIsNormalizedToLowercase() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, "hindi"))
                .thenReturn(List.of(record(now.atDay(3))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, null, null, "  Hindi  ", model);

        assertEquals("hindi", model.get("selectedTag"));
    }

    @Test
    void blankTagIsTreatedAsNoTag() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, null))
                .thenReturn(List.of(record(now.atDay(5))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, null, null, "   ", model);

        assertNull(model.get("selectedTag"));
        assertEquals(1, model.get("totalCount"));
    }

    @Test
    void searchTermExposesResultCountNote() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, "exam", null))
                .thenReturn(List.of(record(now.atDay(3)), record(now.atDay(4))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, null, "exam", null, model);

        assertEquals("2 results for \"exam\"", model.get("searchNote"));
    }

    @Test
    void blankSearchTermIsTreatedAsNoSearch() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, null))
                .thenReturn(List.of(record(now.atDay(5))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, null, "   ", null, model);

        assertNull(model.get("searchTerm"));
        assertNull(model.get("searchNote"));
        assertEquals(1, model.get("totalCount"));
    }

    @Test
    void emptyTypeSelectionMeansAllTypes() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, null))
                .thenReturn(List.of(record(now.atDay(5))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(List.of(), null, null, null, null, model);

        assertNull(model.get("selectedTypes"));
        assertEquals(1, model.get("totalCount"));
    }

    @Test
    void typeFilterCollapsedByDefaultUnlessHiddenTypeSelected() {
        List<CommunicationType> visibleOnly = List.of(CommunicationType.DAILY_LESSON_UPDATE);
        List<CommunicationType> withHidden = List.of(
                CommunicationType.DAILY_LESSON_UPDATE, CommunicationType.REMINDER);

        assertFalse(FeedController.hasSelectedHiddenType(null));
        assertFalse(FeedController.hasSelectedHiddenType(visibleOnly));
        assertTrue(FeedController.hasSelectedHiddenType(withHidden));
    }

    @Test
    void modelExposesTypesExpandedFlag() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        when(repository.findFeed(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        BindingAwareModelMap collapsed = new BindingAwareModelMap();
        controller.feed(List.of(CommunicationType.DAILY_LESSON_UPDATE), null, null, null, null, collapsed);
        assertEquals(false, collapsed.get("typesExpanded"));

        BindingAwareModelMap expanded = new BindingAwareModelMap();
        controller.feed(List.of(CommunicationType.REMINDER), null, null, null, null, expanded);
        assertEquals(true, expanded.get("typesExpanded"));
    }

    @Test
    void academicYearStartResolvesCurrentYearWhenMonthReached() {
        assertEquals(YearMonth.of(2026, 4), FeedController.academicYearStart(YearMonth.of(2026, 4), 4));
        assertEquals(YearMonth.of(2026, 4), FeedController.academicYearStart(YearMonth.of(2026, 8), 4));
        assertEquals(YearMonth.of(2026, 4), FeedController.academicYearStart(YearMonth.of(2026, 12), 4));
    }

    @Test
    void academicYearStartRollsBackToPreviousYearBeforeStartMonth() {
        assertEquals(YearMonth.of(2025, 4), FeedController.academicYearStart(YearMonth.of(2026, 3), 4));
        assertEquals(YearMonth.of(2025, 4), FeedController.academicYearStart(YearMonth.of(2026, 1), 4));
    }

    @Test
    void academicYearStartClampsInvalidMonths() {
        assertEquals(YearMonth.of(2026, 1), FeedController.academicYearStart(YearMonth.of(2026, 3), 0));
        assertEquals(YearMonth.of(2026, 12), FeedController.academicYearStart(YearMonth.of(2026, 12), 13));
    }

    @Test
    void exposesUpcomingEventsAsNoticesOrderedByDate() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        LocalDate today = LocalDate.now();
        LocalDate end = academicStart.plusYears(1).atDay(1).minusDays(1);
        ScheduledEvent exam = scheduledEvent(today.plusDays(2), "Mid-term Exams", ScheduledEventType.EXAM);
        ScheduledEvent holiday = scheduledEvent(today.plusDays(9), "Diwali Break", ScheduledEventType.HOLIDAY);
        when(scheduledEventRepository.findByEventDateBetweenOrderByEventDateAsc(today, end))
                .thenReturn(List.of(exam, holiday));
        when(repository.findFeed(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, null, null, null, model);

        List<FeedController.Notice> notices = noticesOf(model);
        assertEquals(2, notices.size());
        assertEquals(exam, notices.get(0).event());
        assertEquals(holiday, notices.get(1).event());
        assertTrue(notices.get(0).actionable());
        assertFalse(notices.get(1).actionable());
    }

    @Test
    void marksOnlyActionableEventTypes() {
        LocalDate today = LocalDate.now();
        ScheduledEvent exam = scheduledEvent(today.plusDays(2), "Exams", ScheduledEventType.EXAM);
        ScheduledEvent deadline = scheduledEvent(today.plusDays(3), "Fee deadline", ScheduledEventType.SUBMISSION_DEADLINE);
        ScheduledEvent holiday = scheduledEvent(today.plusDays(4), "Holiday", ScheduledEventType.HOLIDAY);
        ScheduledEvent trip = scheduledEvent(today.plusDays(5), "Picnic", ScheduledEventType.FIELD_TRIP);
        when(scheduledEventRepository.findByEventDateBetweenOrderByEventDateAsc(any(), any()))
                .thenReturn(List.of(exam, deadline, holiday, trip));

        YearMonth academicStart = FeedController.academicYearStart(YearMonth.now(), 4);
        List<FeedController.Notice> notices = controller.upcomingNotices(null, null, List.of(), today, academicStart);

        assertTrue(notices.get(0).actionable());
        assertTrue(notices.get(1).actionable());
        assertFalse(notices.get(2).actionable());
        assertFalse(notices.get(3).actionable());
    }

    @Test
    void noticesRespectChatIdFilter() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        LocalDate today = LocalDate.now();
        LocalDate end = academicStart.plusYears(1).atDay(1).minusDays(1);
        Long chat = 42L;
        ScheduledEvent meeting = scheduledEvent(today.plusDays(1), "PTM", ScheduledEventType.PARENT_TEACHER_MEETING);
        when(scheduledEventRepository.findByChatIdAndEventDateBetweenOrderByEventDateAsc(chat, today, end))
                .thenReturn(List.of(meeting));
        when(repository.findFeed(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, chat, null, null, null, model);

        List<FeedController.Notice> notices = noticesOf(model);
        assertEquals(1, notices.size());
        assertEquals(meeting, notices.get(0).event());
        assertTrue(notices.get(0).actionable());
    }

    @Test
    void passesSchoolFilterThroughAndExposesSchools() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        UUID schoolId = UUID.randomUUID();
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, schoolId, null, null))
                .thenReturn(List.of(record(now.atDay(3))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, schoolId, null, null, model);

        assertEquals(schoolId, model.get("selectedSchoolId"));
        assertEquals(1, model.get("totalCount"));
        assertEquals(List.of(), model.get("schools"));
    }

    @Test
    void unknownSchoolIdIsTreatedAsNoSchool() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        UUID schoolId = UUID.randomUUID();
        when(schoolRepository.existsById(schoolId)).thenReturn(false);
        when(repository.findFeed(academicStart.atDay(1), now.atEndOfMonth(), null, null, null, null, null))
                .thenReturn(List.of(record(now.atDay(3))));

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, schoolId, null, null, model);

        assertNull(model.get("selectedSchoolId"));
        assertEquals(1, model.get("totalCount"));
    }

    @Test
    void noticesRespectSchoolFilter() {
        YearMonth now = YearMonth.now();
        YearMonth academicStart = FeedController.academicYearStart(now, 4);
        LocalDate today = LocalDate.now();
        LocalDate end = academicStart.plusYears(1).atDay(1).minusDays(1);
        UUID schoolId = UUID.randomUUID();
        ScheduledEvent meeting = scheduledEvent(today.plusDays(1), "PTM", ScheduledEventType.PARENT_TEACHER_MEETING);
        when(schoolRepository.existsById(schoolId)).thenReturn(true);
        when(appUserRepository.findTelegramChatIdsBySchoolId(schoolId)).thenReturn(List.of(111L));
        when(scheduledEventRepository.findByChatIdInAndEventDateBetweenOrderByEventDateAsc(List.of(111L), today, end))
                .thenReturn(List.of(meeting));
        when(repository.findFeed(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of());

        BindingAwareModelMap model = new BindingAwareModelMap();
        controller.feed(null, null, schoolId, null, null, model);

        List<FeedController.Notice> notices = noticesOf(model);
        assertEquals(1, notices.size());
        assertEquals(meeting, notices.get(0).event());
        assertTrue(notices.get(0).actionable());
    }

    @Test
    void highlightWrapsCaseInsensitiveMatches() {
        FeedSearch search = new FeedSearch();
        assertEquals("Study <mark>exam</mark> topics",
                search.highlight("Study exam topics", "EXAM"));
        assertEquals("<mark>Homework</mark> due <mark>homework</mark>",
                search.highlight("Homework due homework", "homework"));
    }

    @Test
    void highlightEscapesHtmlAndHandlesNoTerm() {
        FeedSearch search = new FeedSearch();
        assertEquals("&lt;b&gt;bold&lt;/b&gt;", search.highlight("<b>bold</b>", null));
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;",
                search.highlight("<script>alert(\"x\")</script>", "no-match"));
        assertEquals("matches <mark>&lt;i&gt;</mark> tag",
                search.highlight("matches <i> tag", "<i>"));
    }

    @Test
    void highlightReturnsNullForNullText() {
        FeedSearch search = new FeedSearch();
        assertNull(search.highlight(null, "exam"));
    }

    private ScheduledEvent scheduledEvent(LocalDate date, String title, ScheduledEventType type) {
        return new ScheduledEvent(UUID.randomUUID(), date, title, type, 12345L, "planner", Instant.now());
    }

    private static List<FeedController.Notice> noticesOf(BindingAwareModelMap model) {
        @SuppressWarnings("unchecked")
        List<FeedController.Notice> notices = (List<FeedController.Notice>) model.get("notices");
        return notices;
    }

    private ClassificationRecord record(LocalDate eventDate) {
        return new ClassificationRecord(UUID.randomUUID(), 12345L, "text",
                CommunicationType.REMINDER, null, eventDate, Instant.now());
    }

    private static List<FeedController.MonthSection> sectionsOf(BindingAwareModelMap model) {
        @SuppressWarnings("unchecked")
        List<FeedController.MonthSection> sections = (List<FeedController.MonthSection>) model.get("sections");
        return sections;
    }
}
