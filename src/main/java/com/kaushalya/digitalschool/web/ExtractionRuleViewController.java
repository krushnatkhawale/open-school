package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.classification.ExtractionField;
import com.kaushalya.digitalschool.classification.ExtractionRuleService;
import com.kaushalya.digitalschool.classification.ExtractionRuleView;
import com.kaushalya.digitalschool.onboarding.SchoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ExtractionRuleViewController {

    private static final Logger log = LoggerFactory.getLogger(ExtractionRuleViewController.class);

    private final ExtractionRuleService ruleService;
    private final AiChatService aiChatService;
    private final SchoolRepository schoolRepository;

    public ExtractionRuleViewController(ExtractionRuleService ruleService,
                                        AiChatService aiChatService,
                                        SchoolRepository schoolRepository) {
        this.ruleService = ruleService;
        this.aiChatService = aiChatService;
        this.schoolRepository = schoolRepository;
    }

    @GetMapping("/extraction-rules")
    public String list(Model model) {
        List<ExtractionRuleView> fields = ruleService.findAll();
        model.addAttribute("fields", fields);
        model.addAttribute("fullPromptAll", buildFullPromptAll(fields));
        model.addAttribute("schools", schoolRepository.findAllByOrderByNameAsc());
        model.addAttribute("selectedSchoolId", null);
        return "extraction-rules";
    }

    @PostMapping("/extraction-rules/{key}")
    public String save(@PathVariable String key,
                       @RequestParam(required = false) String rules,
                       @RequestParam(defaultValue = "false") boolean enabled,
                       RedirectAttributes ra) {
        try {
            ExtractionRuleView view = ruleService.save(key, rules, enabled);
            ra.addFlashAttribute("flash", "Saved " + view.label() + ".");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/extraction-rules";
    }

    @PostMapping("/extraction-rules/{key}/reset")
    public String reset(@PathVariable String key, RedirectAttributes ra) {
        ExtractionRuleView view = ruleService.reset(key);
        ra.addFlashAttribute("flash", "Reset " + view.label() + " to default.");
        return "redirect:/extraction-rules";
    }

    @PostMapping("/extraction-rules/{key}/test")
    public String test(@PathVariable String key,
                       @RequestParam(required = false) String sampleText,
                       @RequestParam(required = false) MultipartFile sampleImage,
                       Model model) {
        List<ExtractionRuleView> fields = ruleService.findAll();
        model.addAttribute("fields", fields);
        model.addAttribute("fullPromptAll", buildFullPromptAll(fields));
        model.addAttribute("schools", schoolRepository.findAllByOrderByNameAsc());
        model.addAttribute("selectedSchoolId", null);

        ExtractionField field = ExtractionField.fromKey(key);
        model.addAttribute("testKey", field.key());
        model.addAttribute("testInput", sampleText);

        try {
            String result = runExtraction(field, sampleText, sampleImage);
            model.addAttribute("testResult", result);
        } catch (Exception e) {
            log.warn("Extraction test failed for field '{}': {}", field.key(), e.getMessage());
            model.addAttribute("testError", "Extraction failed: " + e.getMessage());
        }
        return "extraction-rules";
    }

    private String buildFullPromptAll(List<ExtractionRuleView> fields) {
        return fields.stream()
                .map(field -> "=== " + field.label()
                        + (field.enabled() ? "" : " [disabled]")
                        + " ===\n\n" + field.fullPrompt())
                .collect(Collectors.joining("\n\n"));
    }

    private String runExtraction(ExtractionField field, String sampleText, MultipartFile sampleImage)
            throws IOException {
        byte[] image = sampleImage != null && !sampleImage.isEmpty() ? sampleImage.getBytes() : null;
        String mediaType = sampleImage != null ? sampleImage.getContentType() : null;

        return switch (field) {
            case CLASSIFICATION -> image != null
                    ? aiChatService.classify(sampleText, image, mediaType).name()
                    : aiChatService.classify(sampleText).name();
            case IMAGE_DESCRIPTION -> image != null
                    ? aiChatService.describeImage(image, mediaType)
                    : "Upload an image to test this field.";
            case DATE -> {
                String date = aiChatService.extractDate(sampleText);
                yield date == null ? "NONE" : date;
            }
            case SCHEDULED_EVENTS -> aiChatService.extractScheduledEvents(sampleText).toString();
            case TAGS -> aiChatService.extractTags(sampleText).toString();
        };
    }
}
