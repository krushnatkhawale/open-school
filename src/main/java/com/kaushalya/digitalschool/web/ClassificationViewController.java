package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ClassificationViewController {

    private final ClassificationRecordRepository repository;

    public ClassificationViewController(ClassificationRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/classifications";
    }

    @GetMapping("/classifications")
    public String list(@RequestParam(required = false) Long chatId,
                       @RequestParam(required = false) CommunicationType type,
                       Model model) {
        var records = (chatId != null)
                ? repository.findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(chatId)
                : (type != null)
                ? repository.findByCommunicationTypeAndFeedHiddenFalseOrderByUploadedAtDesc(type)
                : repository.findAllByFeedHiddenFalseOrderByUploadedAtDesc();
        model.addAttribute("records", records);
        model.addAttribute("types", CommunicationType.values());
        model.addAttribute("selectedChatId", chatId);
        model.addAttribute("selectedType", type);
        return "classifications";
    }
}
