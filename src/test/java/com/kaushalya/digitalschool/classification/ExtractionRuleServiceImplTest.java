package com.kaushalya.digitalschool.classification;

import com.kaushalya.digitalschool.storage.ExtractionRule;
import com.kaushalya.digitalschool.storage.ExtractionRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionRuleServiceImplTest {

    @Mock
    private ExtractionRuleRepository repository;

    @InjectMocks
    private ExtractionRuleServiceImpl service;

    @Test
    void findAllMergesDefaultsForMissingRows() {
        when(repository.findAll()).thenReturn(List.of());

        List<ExtractionRuleView> views = service.findAll();

        assertEquals(ExtractionField.values().length, views.size());
        assertTrue(views.stream().allMatch(ExtractionRuleView::usingDefault));
        assertEquals(ExtractionField.DATE.defaultRules(),
                views.stream().filter(v -> v.key().equals("date")).findFirst().orElseThrow().rules());
    }

    @Test
    void findAllMergesStoredRules() {
        ExtractionRule stored = new ExtractionRule("date", "CUSTOM DATE RULES", true);
        when(repository.findAll()).thenReturn(List.of(stored));

        List<ExtractionRuleView> views = service.findAll();

        ExtractionRuleView date = views.stream().filter(v -> v.key().equals("date")).findFirst().orElseThrow();
        assertFalse(date.usingDefault());
        assertEquals("CUSTOM DATE RULES", date.rules());
        assertTrue(date.fullPrompt().contains("CUSTOM DATE RULES"));
        assertEquals(ExtractionField.CLASSIFICATION.defaultRules(),
                views.stream().filter(v -> v.key().equals("classification")).findFirst().orElseThrow().rules());
    }

    @Test
    void findByKeyUsesDefaultWhenNoRowStored() {
        when(repository.findById("date")).thenReturn(Optional.empty());

        ExtractionRuleView view = service.findByKey("date");

        assertTrue(view.usingDefault());
        assertTrue(view.enabled());
        assertEquals(ExtractionField.DATE.defaultRules(), view.rules());
    }

    @Test
    void savePersistsRulesAndEnabled() {
        when(repository.findById("tags")).thenReturn(Optional.empty());
        when(repository.save(any(ExtractionRule.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionRuleView view = service.save("tags", " maths , hindi ", true);

        assertFalse(view.usingDefault());
        assertTrue(view.enabled());
        assertEquals("maths , hindi", view.rules());
        verify(repository).save(any(ExtractionRule.class));
    }

    @Test
    void saveRejectsBlankRulesWhenEnabled() {
        assertThrows(IllegalArgumentException.class, () -> service.save("tags", "   ", true));
    }

    @Test
    void saveAllowsBlankRulesWhenDisabled() {
        when(repository.findById("tags")).thenReturn(Optional.empty());
        when(repository.save(any(ExtractionRule.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionRuleView view = service.save("tags", "", false);

        assertFalse(view.enabled());
        assertTrue(view.usingDefault());
    }

    @Test
    void saveUpdatesExistingRowInPlace() {
        ExtractionRule existing = new ExtractionRule("date", "OLD", true);
        when(repository.findById("date")).thenReturn(Optional.of(existing));
        when(repository.save(any(ExtractionRule.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionRuleView view = service.save("date", "NEW RULES", true);

        assertEquals("NEW RULES", existing.getRules());
        assertTrue(existing.isEnabled());
        assertEquals("NEW RULES", view.rules());
        assertTrue(view.enabled());
    }

    @Test
    void resetRemovesStoredOverride() {
        ExtractionRuleView view = service.reset("date");

        assertTrue(view.usingDefault());
        assertTrue(view.enabled());
        assertEquals(ExtractionField.DATE.defaultRules(), view.rules());
        verify(repository).deleteById("date");
    }

    @Test
    void saveNormalizesLineEndings() {
        when(repository.findById("date")).thenReturn(Optional.empty());
        when(repository.save(any(ExtractionRule.class))).thenAnswer(inv -> inv.getArgument(0));

        ExtractionRuleView view = service.save("date", "first line\r\nsecond line\rthird line", true);

        assertEquals("first line\nsecond line\nthird line", view.rules());
        verify(repository).save(any(ExtractionRule.class));
    }

    @Test
    void findAllNormalizesLineEndingsForStoredRules() {
        ExtractionRule stored = new ExtractionRule("date", "line one\r\nline two", true);
        when(repository.findAll()).thenReturn(List.of(stored));

        ExtractionRuleView view = service.findAll().stream()
                .filter(v -> v.key().equals("date")).findFirst().orElseThrow();

        assertEquals("line one\nline two", view.rules());
        assertEquals("line one\nline two", view.fullPrompt().substring(0, "line one\nline two".length()));
    }

    @Test
    void systemPromptForNormalizesLineEndings() {
        when(repository.findById(ExtractionField.DATE.key())).thenReturn(
                Optional.of(new ExtractionRule("date", "first\r\nsecond", true)));

        String prompt = service.systemPromptFor(ExtractionField.DATE);

        assertTrue(prompt.contains("first\nsecond"));
        assertFalse(prompt.contains("\r"));
    }

    @Test
    void systemPromptForUsesStoredRulesWhenEnabled() {
        when(repository.findById(ExtractionField.DATE.key())).thenReturn(
                Optional.of(new ExtractionRule("date", "CUSTOM DATE RULES", true)));

        String prompt = service.systemPromptFor(ExtractionField.DATE);

        assertTrue(prompt.contains("CUSTOM DATE RULES"));
        assertTrue(prompt.contains("YYYY-MM-DD"));
    }

    @Test
    void systemPromptForFallsBackToDefaultWhenDisabled() {
        when(repository.findById(ExtractionField.DATE.key())).thenReturn(
                Optional.of(new ExtractionRule("date", "CUSTOM", false)));

        String prompt = service.systemPromptFor(ExtractionField.DATE);

        assertTrue(prompt.contains(ExtractionField.DATE.defaultRules()));
        assertFalse(prompt.contains("CUSTOM"));
    }

    @Test
    void systemPromptForFallsBackToDefaultWhenMissing() {
        when(repository.findById(ExtractionField.DATE.key())).thenReturn(Optional.empty());

        String prompt = service.systemPromptFor(ExtractionField.DATE);

        assertEquals(ExtractionField.DATE.defaultRules() + "\n\n" + ExtractionField.DATE.contract(), prompt);
    }

    @Test
    void imageDescriptionPromptPlacesContractFirst() {
        when(repository.findById(ExtractionField.IMAGE_DESCRIPTION.key())).thenReturn(Optional.empty());

        String prompt = service.systemPromptFor(ExtractionField.IMAGE_DESCRIPTION);

        assertTrue(prompt.startsWith(ExtractionField.IMAGE_DESCRIPTION.contract()));
        assertTrue(prompt.endsWith(ExtractionField.IMAGE_DESCRIPTION.defaultRules()));
    }

    @Test
    void unknownKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> service.findByKey("bogus"));
        assertThrows(IllegalArgumentException.class, () -> service.save("bogus", "x", true));
    }
}
