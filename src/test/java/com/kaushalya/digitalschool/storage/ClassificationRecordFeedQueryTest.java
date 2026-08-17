package com.kaushalya.digitalschool.storage;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.onboarding.AppUser;
import com.kaushalya.digitalschool.onboarding.AppUserRepository;
import com.kaushalya.digitalschool.onboarding.OnboardingStatus;
import com.kaushalya.digitalschool.onboarding.School;
import com.kaushalya.digitalschool.onboarding.SchoolRepository;
import com.kaushalya.digitalschool.onboarding.UserRole;
import com.kaushalya.digitalschool.shared.CommunicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "spring.datasource.url=jdbc:h2:mem:feedtest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ClassificationRecordFeedQueryTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        AiChatService aiChatService() {
            return Mockito.mock(AiChatService.class);
        }
    }

    @Autowired
    private ClassificationRecordRepository repository;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @BeforeEach
    void cleanDb() {
        repository.deleteAll();
        appUserRepository.deleteAll();
        schoolRepository.deleteAll();
    }

    @Test
    void feedQueryReturnsOnlyRecordsInMonthOrderedByEventDateDesc() {
        repository.save(record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L));
        repository.save(record(LocalDate.of(2026, 7, 20), CommunicationType.REMINDER, 222L));
        repository.save(record(LocalDate.of(2026, 6, 30), CommunicationType.DAILY_LESSON_UPDATE, 111L));
        repository.save(record(LocalDate.of(2026, 7, 25), CommunicationType.EVENT, 111L));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, null, null);

        assertEquals(List.of(LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 5)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryFiltersByType() {
        repository.save(record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L));
        repository.save(record(LocalDate.of(2026, 7, 20), CommunicationType.REMINDER, 222L));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), List.of(CommunicationType.REMINDER), null, null, null, null);

        assertEquals(List.of(LocalDate.of(2026, 7, 20)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryFiltersByMultipleTypes() {
        repository.save(record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L));
        repository.save(record(LocalDate.of(2026, 7, 20), CommunicationType.REMINDER, 222L));
        repository.save(record(LocalDate.of(2026, 7, 25), CommunicationType.EVENT, 111L));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                List.of(CommunicationType.REMINDER, CommunicationType.EVENT), null, null, null, null);

        assertEquals(List.of(LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 20)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryFiltersByChatId() {
        repository.save(record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L));
        repository.save(record(LocalDate.of(2026, 7, 20), CommunicationType.REMINDER, 222L));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, 111L, null, null, null);

        assertEquals(List.of(LocalDate.of(2026, 7, 5)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQuerySpansAcademicYearMonthsOrderedNewestFirst() {
        repository.save(record(LocalDate.of(2026, 4, 10), CommunicationType.DAILY_LESSON_UPDATE, 111L));
        repository.save(record(LocalDate.of(2026, 7, 5), CommunicationType.REMINDER, 222L));
        repository.save(record(LocalDate.of(2026, 3, 1), CommunicationType.EVENT, 111L));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 7, 31), null, null, null, null, null);

        assertEquals(List.of(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 4, 10)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryFiltersBySearchTermCaseInsensitiveInTextOrDescription() {
        repository.save(recordWithText(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE,
                111L, "Homework: solve the fraction sums", "page 12 worksheet"));
        repository.save(recordWithText(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER,
                222L, "Bring school fee slip", "payment due Friday"));
        repository.save(recordWithText(LocalDate.of(2026, 7, 7), CommunicationType.EVENT,
                111L, "Annual sports day", "fraction race event description"));

        List<ClassificationRecord> byText = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, "fraction", null);
        assertEquals(List.of(LocalDate.of(2026, 7, 7), LocalDate.of(2026, 7, 5)),
                byText.stream().map(ClassificationRecord::getEventDate).toList());

        List<ClassificationRecord> byDescription = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, "FRIDAY", null);
        assertEquals(List.of(LocalDate.of(2026, 7, 6)),
                byDescription.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryCombinesSearchTermWithOtherFilters() {
        repository.save(recordWithText(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE,
                111L, "Homework: fraction sums", "page 12"));
        repository.save(recordWithText(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER,
                111L, "Homework: bring library books", "notes"));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                List.of(CommunicationType.DAILY_LESSON_UPDATE), 111L, null, "homework", null);

        assertEquals(List.of(LocalDate.of(2026, 7, 5)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryMatchesSearchTermInTags() {
        repository.save(recordWithTags(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE,
                111L, "Notebook page about fractions", "Page 12", "story,hindi"));
        repository.save(recordWithText(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER,
                222L, "Bring fee slip tomorrow", "Payment due"));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, "story", null);

        assertEquals(List.of(LocalDate.of(2026, 7, 5)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryFiltersByExactTag() {
        repository.save(recordWithTags(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE,
                111L, "Notebook page", "Page 12", "story,hindi"));
        repository.save(recordWithTags(LocalDate.of(2026, 7, 6), CommunicationType.EVENT,
                222L, "Annual day", "stage setup", "story"));
        repository.save(recordWithText(LocalDate.of(2026, 7, 7), CommunicationType.REMINDER,
                333L, "Fee payment", "due Friday"));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, null, "story");

        assertEquals(List.of(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 5)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryTagFilterMatchesWholeTagNotSubstring() {
        repository.save(recordWithTags(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE,
                111L, "Notebook page", "Page 12", "hindi"));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, null, "hi");

        assertEquals(List.of(), result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    @Test
    void feedQueryExcludesFeedHiddenRecords() {
        ClassificationRecord visible = record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L);
        ClassificationRecord hidden = record(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER, 222L);
        hidden.setFeedHidden(true);
        repository.save(visible);
        repository.save(hidden);

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, null, null);

        assertEquals(List.of(visible.getId()), result.stream().map(ClassificationRecord::getId).toList());
    }

    @Test
    void listQueryExcludesFeedHiddenRecords() {
        ClassificationRecord visible = record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L);
        ClassificationRecord hidden = record(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER, 222L);
        hidden.setFeedHidden(true);
        repository.save(visible);
        repository.save(hidden);

        List<ClassificationRecord> result = repository.findAllByFeedHiddenFalseOrderByUploadedAtDesc();

        assertEquals(List.of(visible.getId()), result.stream().map(ClassificationRecord::getId).toList());
    }

    @Test
    void listQueryByChatIdExcludesFeedHiddenRecords() {
        ClassificationRecord visible = record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L);
        ClassificationRecord hidden = record(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER, 111L);
        hidden.setFeedHidden(true);
        repository.save(visible);
        repository.save(hidden);

        List<ClassificationRecord> result = repository.findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(111L);

        assertEquals(List.of(visible.getId()), result.stream().map(ClassificationRecord::getId).toList());
    }

    @Test
    void listQueryByTypeExcludesFeedHiddenRecords() {
        ClassificationRecord visible = record(LocalDate.of(2026, 7, 5), CommunicationType.REMINDER, 111L);
        ClassificationRecord hidden = record(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER, 222L);
        hidden.setFeedHidden(true);
        repository.save(visible);
        repository.save(hidden);

        List<ClassificationRecord> result = repository.findByCommunicationTypeAndFeedHiddenFalseOrderByUploadedAtDesc(CommunicationType.REMINDER);

        assertEquals(List.of(visible.getId()), result.stream().map(ClassificationRecord::getId).toList());
    }

    @Test
    void feedQueryFiltersBySchoolThroughUserChatIds() {
        School school = schoolRepository.save(new School(UUID.randomUUID(), "Green Valley", null, null, Instant.now()));
        School other = schoolRepository.save(new School(UUID.randomUUID(), "Sunrise", null, null, Instant.now()));

        AppUser member = new AppUser(UUID.randomUUID(), 111L, UserRole.SCHOOL, OnboardingStatus.ACTIVE, Instant.now());
        member.setSchool(school);
        appUserRepository.save(member);
        AppUser otherMember = new AppUser(UUID.randomUUID(), 222L, UserRole.SCHOOL, OnboardingStatus.ACTIVE, Instant.now());
        otherMember.setSchool(other);
        appUserRepository.save(otherMember);

        repository.save(record(LocalDate.of(2026, 7, 5), CommunicationType.DAILY_LESSON_UPDATE, 111L));
        repository.save(record(LocalDate.of(2026, 7, 6), CommunicationType.REMINDER, 222L));

        List<ClassificationRecord> result = repository.findFeed(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, school.getId(), null, null);

        assertEquals(List.of(LocalDate.of(2026, 7, 5)),
                result.stream().map(ClassificationRecord::getEventDate).toList());
    }

    private ClassificationRecord record(LocalDate eventDate, CommunicationType type, long chatId) {
        return new ClassificationRecord(UUID.randomUUID(), chatId, "text", type,
                "desc", eventDate, Instant.now());
    }

    private ClassificationRecord recordWithText(LocalDate eventDate, CommunicationType type, long chatId,
                                                String text, String description) {
        return new ClassificationRecord(UUID.randomUUID(), chatId, text, type,
                description, eventDate, Instant.now());
    }

    private ClassificationRecord recordWithTags(LocalDate eventDate, CommunicationType type, long chatId,
                                                String text, String description, String tags) {
        ClassificationRecord record = new ClassificationRecord(UUID.randomUUID(), chatId, text, type,
                description, eventDate, Instant.now());
        record.setTags(tags);
        return record;
    }
}
