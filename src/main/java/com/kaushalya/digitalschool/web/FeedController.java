package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.onboarding.AppUserRepository;
import com.kaushalya.digitalschool.onboarding.School;
import com.kaushalya.digitalschool.onboarding.SchoolRepository;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ScheduledEventType;
import com.kaushalya.digitalschool.shared.SchoolProperties;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import com.kaushalya.digitalschool.storage.ScheduledEvent;
import com.kaushalya.digitalschool.storage.ScheduledEventRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Controller
public class FeedController {

    private static final Set<ScheduledEventType> ACTIONABLE_TYPES = EnumSet.of(
            ScheduledEventType.EXAM,
            ScheduledEventType.SUBMISSION_DEADLINE,
            ScheduledEventType.PARENT_TEACHER_MEETING);

    private static final int VISIBLE_TYPE_FILTERS = 4;

    private final ClassificationRecordRepository repository;
    private final ScheduledEventRepository scheduledEventRepository;
    private final SchoolProperties properties;
    private final FeedSearch feedSearch;
    private final FeedLinks feedLinks;
    private final SchoolRepository schoolRepository;
    private final AppUserRepository appUserRepository;

    public FeedController(ClassificationRecordRepository repository,
                          ScheduledEventRepository scheduledEventRepository,
                          SchoolProperties properties,
                          FeedSearch feedSearch,
                          FeedLinks feedLinks,
                          SchoolRepository schoolRepository,
                          AppUserRepository appUserRepository) {
        this.repository = repository;
        this.scheduledEventRepository = scheduledEventRepository;
        this.properties = properties;
        this.feedSearch = feedSearch;
        this.feedLinks = feedLinks;
        this.schoolRepository = schoolRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(required = false) List<CommunicationType> types,
                       @RequestParam(required = false) Long chatId,
                       @RequestParam(required = false) UUID schoolId,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String tag,
                       Model model) {
        YearMonth current = YearMonth.now();
        YearMonth academicStart = academicYearStart(current, properties.academicYearStartMonth());

        List<CommunicationType> selectedTypes = types == null || types.isEmpty() ? null : types;
        String searchTerm = normalizeSearchTerm(q);
        String selectedTag = normalizeTag(tag);
        UUID selectedSchoolId = validSchoolId(schoolId);
        List<ClassificationRecord> records = repository.findFeed(
                academicStart.atDay(1), current.atEndOfMonth(), selectedTypes, chatId, selectedSchoolId, searchTerm, selectedTag);

        List<MonthSection> sections = groupByMonth(current, academicStart, records);

        int totalCount = 0;
        for (MonthSection section : sections) {
            totalCount += section.records().size();
        }

        Map<UUID, String> highlightedText = new HashMap<>();
        Map<UUID, String> highlightedDescription = new HashMap<>();
        for (ClassificationRecord r : records) {
            highlightedText.put(r.getId(), feedSearch.highlight(r.getOriginalText(), searchTerm));
            if (r.getAiDescription() != null) {
                highlightedDescription.put(r.getId(), feedSearch.highlight(r.getAiDescription(), searchTerm));
            }
        }

        List<Long> schoolChatIds = selectedSchoolId == null ? List.of() : appUserRepository.findTelegramChatIdsBySchoolId(selectedSchoolId);
        model.addAttribute("notices", upcomingNotices(chatId, selectedSchoolId, schoolChatIds, LocalDate.now(), academicStart));
        model.addAttribute("sections", sections);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("types", CommunicationType.values());
        model.addAttribute("typesExpanded", hasSelectedHiddenType(selectedTypes));
        model.addAttribute("selectedTypes", selectedTypes);
        model.addAttribute("selectedChatId", chatId);
        model.addAttribute("selectedTag", selectedTag);
        model.addAttribute("selectedSchoolId", selectedSchoolId);
        model.addAttribute("schools", schoolRepository.findAllByOrderByNameAsc());
        model.addAttribute("searchTerm", searchTerm);
        model.addAttribute("searchNote", searchNote(searchTerm, totalCount));
        model.addAttribute("feedLinks", feedLinks);
        model.addAttribute("highlightedText", highlightedText);
        model.addAttribute("highlightedDescription", highlightedDescription);
        return "feed";
    }

    UUID validSchoolId(UUID schoolId) {
        if (schoolId == null) {
            return null;
        }
        return schoolRepository.existsById(schoolId) ? schoolId : null;
    }

    static String normalizeSearchTerm(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeTag(String tag) {
        if (tag == null) {
            return null;
        }
        String trimmed = tag.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    static String searchNote(String searchTerm, int totalCount) {
        if (searchTerm == null) {
            return null;
        }
        String count = totalCount == 1 ? "1 result" : totalCount + " results";
        return count + " for \"" + searchTerm + "\"";
    }

    static boolean hasSelectedHiddenType(List<CommunicationType> selectedTypes) {
        if (selectedTypes == null) {
            return false;
        }
        CommunicationType[] all = CommunicationType.values();
        for (int i = VISIBLE_TYPE_FILTERS; i < all.length; i++) {
            if (selectedTypes.contains(all[i])) {
                return true;
            }
        }
        return false;
    }

    List<Notice> upcomingNotices(Long chatId, UUID schoolId, List<Long> schoolChatIds,
                                 LocalDate from, YearMonth academicStart) {
        LocalDate to = academicStart.plusYears(1).atDay(1).minusDays(1);
        List<ScheduledEvent> upcoming;
        if (chatId != null) {
            upcoming = scheduledEventRepository.findByChatIdAndEventDateBetweenOrderByEventDateAsc(chatId, from, to);
        } else if (schoolId != null) {
            upcoming = scheduledEventRepository.findByChatIdInAndEventDateBetweenOrderByEventDateAsc(schoolChatIds, from, to);
        } else {
            upcoming = scheduledEventRepository.findByEventDateBetweenOrderByEventDateAsc(from, to);
        }
        return upcoming.stream()
                .map(e -> new Notice(e, ACTIONABLE_TYPES.contains(e.getEventType())))
                .toList();
    }

    public record Notice(ScheduledEvent event, boolean actionable) {
    }

    static YearMonth academicYearStart(YearMonth current, int startMonth) {
        int m = Math.min(12, Math.max(1, startMonth));
        return current.getMonthValue() >= m
                ? YearMonth.of(current.getYear(), m)
                : YearMonth.of(current.getYear() - 1, m);
    }

    static List<MonthSection> groupByMonth(YearMonth current, YearMonth academicStart,
                                           List<ClassificationRecord> records) {
        Map<YearMonth, List<ClassificationRecord>> byMonth = new LinkedHashMap<>();
        for (ClassificationRecord r : records) {
            if (r.getEventDate() == null) {
                continue;
            }
            byMonth.computeIfAbsent(YearMonth.from(r.getEventDate()), k -> new ArrayList<>()).add(r);
        }

        List<MonthSection> sections = new ArrayList<>();
        for (YearMonth ym = current; !ym.isBefore(academicStart); ym = ym.minusMonths(1)) {
            sections.add(MonthSection.of(ym, byMonth.getOrDefault(ym, List.of())));
        }
        return sections;
    }

    public record MonthSection(String monthLabel, int year, int month, int daysInMonth,
                               List<ClassificationRecord> records, TreeSet<Integer> daysWithPosts) {

        static MonthSection of(YearMonth ym, List<ClassificationRecord> monthRecords) {
            TreeSet<Integer> days = new TreeSet<>();
            for (ClassificationRecord r : monthRecords) {
                if (r.getEventDate() != null) {
                    days.add(r.getEventDate().getDayOfMonth());
                }
            }
            return new MonthSection(
                    ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear(),
                    ym.getYear(), ym.getMonthValue(), ym.lengthOfMonth(), monthRecords, days);
        }
    }
}
