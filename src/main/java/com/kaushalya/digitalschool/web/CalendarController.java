package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import com.kaushalya.digitalschool.storage.ScheduledEvent;
import com.kaushalya.digitalschool.storage.ScheduledEventRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/calendar")
public class CalendarController {

    private final ClassificationRecordRepository classificationRepository;
    private final ScheduledEventRepository scheduledEventRepository;

    public CalendarController(ClassificationRecordRepository classificationRepository,
                              ScheduledEventRepository scheduledEventRepository) {
        this.classificationRepository = classificationRepository;
        this.scheduledEventRepository = scheduledEventRepository;
    }

    @GetMapping
    public String calendar(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month,
                           Model model) {
        YearMonth current = YearMonth.now();
        int y = year != null ? year : current.getYear();
        int m = month != null ? month : current.getMonthValue();
        YearMonth yearMonth = YearMonth.of(y, m);

        Set<LocalDate> daysWithClassifications = classificationRepository.findByFeedHiddenFalse().stream()
                .map(ClassificationRecord::getEventDate)
                .filter(d -> d != null && YearMonth.from(d).equals(yearMonth))
                .collect(Collectors.toSet());

        Set<LocalDate> daysWithScheduled = scheduledEventRepository
                .findByEventDateBetweenOrderByEventDateAsc(yearMonth.atDay(1), yearMonth.atEndOfMonth())
                .stream()
                .map(ScheduledEvent::getEventDate)
                .collect(Collectors.toSet());

        LocalDate firstOfMonth = yearMonth.atDay(1);
        int dowOffset = (firstOfMonth.getDayOfWeek().getValue() % 7);

        List<List<CalendarDay>> weeks = new ArrayList<>();
        List<CalendarDay> week = new ArrayList<>();
        for (int i = 0; i < dowOffset; i++) {
            week.add(new CalendarDay(0, false, false, null));
        }
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            week.add(new CalendarDay(day, daysWithClassifications.contains(date),
                    daysWithScheduled.contains(date), date.toString()));
            if (week.size() == 7) {
                weeks.add(week);
                week = new ArrayList<>();
            }
        }
        if (!week.isEmpty()) {
            while (week.size() < 7) {
                week.add(new CalendarDay(0, false, false, null));
            }
            weeks.add(week);
        }

        YearMonth prev = yearMonth.minusMonths(1);
        YearMonth next = yearMonth.plusMonths(1);

        model.addAttribute("weeks", weeks);
        model.addAttribute("year", y);
        model.addAttribute("month", m);
        model.addAttribute("monthName", yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        model.addAttribute("prevYear", prev.getYear());
        model.addAttribute("prevMonth", prev.getMonthValue());
        model.addAttribute("nextYear", next.getYear());
        model.addAttribute("nextMonth", next.getMonthValue());
        return "calendar";
    }

    @GetMapping("/day")
    public String day(@RequestParam String date, Model model) {
        LocalDate day = LocalDate.parse(date);
        List<ClassificationRecord> records = classificationRepository.findByFeedHiddenFalseAndEventDateOrderByUploadedAtDesc(day);
        List<ScheduledEvent> scheduledEvents = scheduledEventRepository.findByEventDateOrderByEventTypeAsc(day);
        model.addAttribute("records", records);
        model.addAttribute("scheduledEvents", scheduledEvents);
        model.addAttribute("date", date);
        model.addAttribute("types", CommunicationType.values());
        return "day";
    }

    public record CalendarDay(int day, boolean hasClassifications, boolean hasScheduledEvents, String dateStr) {
    }
}
