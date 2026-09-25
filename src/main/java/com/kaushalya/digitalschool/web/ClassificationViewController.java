package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.onboarding.AppUserRepository;
import com.kaushalya.digitalschool.onboarding.SchoolRepository;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Controller
public class ClassificationViewController {

    private final ClassificationRecordRepository repository;
    private final SchoolRepository schoolRepository;
    private final AppUserRepository appUserRepository;

    public ClassificationViewController(ClassificationRecordRepository repository,
                                        SchoolRepository schoolRepository,
                                        AppUserRepository appUserRepository) {
        this.repository = repository;
        this.schoolRepository = schoolRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/classifications";
    }

    @GetMapping("/classifications")
    public String list(@RequestParam(required = false) Long chatId,
                       @RequestParam(required = false) CommunicationType type,
                       @RequestParam(required = false) UUID schoolId,
                       Model model) {
        UUID selectedSchoolId = validSchoolId(schoolId);
        List<Long> schoolChatIds = schoolChatIds(selectedSchoolId);

        List<ClassificationRecord> records = (chatId != null)
                ? repository.findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(chatId)
                : (type != null)
                ? repository.findByCommunicationTypeAndFeedHiddenFalseOrderByUploadedAtDesc(type)
                : repository.findAllByFeedHiddenFalseOrderByUploadedAtDesc();

        if (!schoolChatIds.isEmpty()) {
            records = records.stream()
                    .filter(r -> r.getChatId() != null && schoolChatIds.contains(r.getChatId()))
                    .toList();
        }

        model.addAttribute("records", records);
        model.addAttribute("types", CommunicationType.values());
        model.addAttribute("selectedChatId", chatId);
        model.addAttribute("selectedType", type);
        model.addAttribute("schools", schoolRepository.findAllByOrderByNameAsc());
        model.addAttribute("selectedSchoolId", selectedSchoolId);
        return "classifications";
    }

    private UUID validSchoolId(UUID schoolId) {
        if (schoolId == null) {
            return null;
        }
        return schoolRepository.existsById(schoolId) ? schoolId : null;
    }

    private List<Long> schoolChatIds(UUID schoolId) {
        if (schoolId == null) {
            return List.of();
        }
        return appUserRepository.findTelegramChatIdsBySchoolId(schoolId);
    }
}
