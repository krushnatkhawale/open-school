package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.classification.ExtractionRuleService;
import com.kaushalya.digitalschool.classification.ExtractionRuleView;
import com.kaushalya.digitalschool.shared.CommunicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionRuleViewControllerTest {

    @Mock
    private ExtractionRuleService ruleService;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private Model model;

    @Mock
    private RedirectAttributes redirectAttributes;

    private ExtractionRuleViewController controller;

    @BeforeEach
    void setUp() {
        controller = new ExtractionRuleViewController(ruleService, aiChatService);
    }

    @Test
    void listAddsFieldsToModelAndReturnsView() {
        when(ruleService.findAll()).thenReturn(List.of(view()));

        String view = controller.list(model);

        assertEquals("extraction-rules", view);
        verify(model).addAttribute("fields", List.of(view()));
    }

    @Test
    void saveRedirectsAndAddsFlash() {
        ExtractionRuleView saved = view();
        when(ruleService.save("date", "RULES", true)).thenReturn(saved);

        String result = controller.save("date", "RULES", true, redirectAttributes);

        assertEquals("redirect:/extraction-rules", result);
        verify(redirectAttributes).addFlashAttribute("flash", "Saved Lesson date.");
    }

    @Test
    void saveAddsFlashErrorWhenRulesBlankWhileEnabled() {
        when(ruleService.save("date", "   ", true))
                .thenThrow(new IllegalArgumentException("Rules cannot be blank while the field is enabled."));

        String result = controller.save("date", "   ", true, redirectAttributes);

        assertEquals("redirect:/extraction-rules", result);
        verify(redirectAttributes).addFlashAttribute("flashError",
                "Rules cannot be blank while the field is enabled.");
    }

    @Test
    void resetRedirectsAndAddsFlash() {
        when(ruleService.reset("tags")).thenReturn(tagsView());

        String result = controller.reset("tags", redirectAttributes);

        assertEquals("redirect:/extraction-rules", result);
        verify(redirectAttributes).addFlashAttribute("flash", "Reset Tags to default.");
    }

    @Test
    void testRunsDateExtractionAndRendersResult() {
        when(ruleService.findAll()).thenReturn(List.of(view()));
        when(aiChatService.extractDate("A notebook page")).thenReturn("2026-08-01");

        String result = controller.test("date", "A notebook page", null, model);

        assertEquals("extraction-rules", result);
        verify(model).addAttribute("testKey", "date");
        verify(model).addAttribute("testResult", "2026-08-01");
    }

    @Test
    void testClassifiesTextAndRendersCategoryName() {
        when(ruleService.findAll()).thenReturn(List.of(view()));
        when(aiChatService.classify("Sign the form")).thenReturn(CommunicationType.ACTION_REQUIRED);

        String result = controller.test("classification", "Sign the form", null, model);

        assertEquals("extraction-rules", result);
        verify(model).addAttribute("testResult", "ACTION_REQUIRED");
    }

    @Test
    void testClassifiesUploadedImageWithCaption() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "sampleImage", "page.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(ruleService.findAll()).thenReturn(List.of(view()));
        when(aiChatService.classify(eq("caption"), any(byte[].class), eq("image/jpeg")))
                .thenReturn(CommunicationType.EVENT);

        String result = controller.test("classification", "caption", image, model);

        assertEquals("extraction-rules", result);
        verify(model).addAttribute("testResult", "EVENT");
    }

    @Test
    void testImageDescriptionWithoutImageReturnsGuidance() {
        when(ruleService.findAll()).thenReturn(List.of(view()));

        String result = controller.test("imageDescription", null, null, model);

        assertEquals("extraction-rules", result);
        verify(model).addAttribute("testResult", "Upload an image to test this field.");
    }

    @Test
    void testCapturesExtractionErrors() {
        when(ruleService.findAll()).thenReturn(List.of(view()));
        when(aiChatService.extractDate(any())).thenThrow(new RuntimeException("model down"));

        String result = controller.test("date", "sample", null, model);

        assertEquals("extraction-rules", result);
        verify(model).addAttribute("testError", "Extraction failed: model down");
    }

    private ExtractionRuleView view() {
        return new ExtractionRuleView("date", "Lesson date", "desc", "RULES", true, false, null, "PROMPT");
    }

    private ExtractionRuleView tagsView() {
        return new ExtractionRuleView("tags", "Tags", "desc", "RULES", true, false, null, "PROMPT");
    }
}
