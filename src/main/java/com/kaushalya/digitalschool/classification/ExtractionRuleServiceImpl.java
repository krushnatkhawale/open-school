package com.kaushalya.digitalschool.classification;

import com.kaushalya.digitalschool.storage.ExtractionRule;
import com.kaushalya.digitalschool.storage.ExtractionRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExtractionRuleServiceImpl implements ExtractionRuleService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionRuleServiceImpl.class);

    private final ExtractionRuleRepository repository;

    public ExtractionRuleServiceImpl(ExtractionRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtractionRuleView> findAll() {
        Map<String, ExtractionRule> stored = repository.findAll().stream()
                .collect(Collectors.toMap(ExtractionRule::getKey, r -> r));
        return Arrays.stream(ExtractionField.values())
                .map(field -> toView(field, stored.get(field.key())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExtractionRuleView findByKey(String key) {
        ExtractionField field = ExtractionField.fromKey(key);
        return toView(field, repository.findById(field.key()).orElse(null));
    }

    @Override
    @Transactional
    public ExtractionRuleView save(String key, String rules, boolean enabled) {
        ExtractionField field = ExtractionField.fromKey(key);
        String trimmed = normalizeLineEndings(rules) == null ? "" : normalizeLineEndings(rules).trim();
        if (enabled && trimmed.isBlank()) {
            throw new IllegalArgumentException("Rules cannot be blank while the field is enabled.");
        }

        ExtractionRule entity = repository.findById(field.key())
                .map(existing -> {
                    existing.setRules(trimmed);
                    existing.setEnabled(enabled);
                    existing.setUpdatedAt(Instant.now());
                    return existing;
                })
                .orElseGet(() -> new ExtractionRule(field.key(), trimmed, enabled));

        log.info("Saved extraction rules for field '{}' enabled={} chars={}", field.key(), enabled, trimmed.length());
        return toView(field, repository.save(entity));
    }

    @Override
    @Transactional
    public ExtractionRuleView reset(String key) {
        ExtractionField field = ExtractionField.fromKey(key);
        repository.deleteById(field.key());
        log.info("Reset extraction rules for field '{}' to default", field.key());
        return toView(field, null);
    }

    @Override
    @Transactional(readOnly = true)
    public String systemPromptFor(ExtractionField field) {
        return field.prompt(effectiveRules(field));
    }

    private String effectiveRules(ExtractionField field) {
        return repository.findById(field.key())
                .filter(ExtractionRule::isEnabled)
                .map(ExtractionRule::getRules)
                .map(ExtractionRuleServiceImpl::normalizeLineEndings)
                .filter(rules -> rules != null && !rules.isBlank())
                .orElse(field.defaultRules());
    }

    private static String normalizeLineEndings(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private ExtractionRuleView toView(ExtractionField field, ExtractionRule rule) {
        boolean usingDefault = rule == null || !rule.isEnabled()
                || rule.getRules() == null || rule.getRules().isBlank();
        String rules = usingDefault ? field.defaultRules() : normalizeLineEndings(rule.getRules());
        return new ExtractionRuleView(
                field.key(),
                field.label(),
                field.description(),
                rules,
                rule == null || rule.isEnabled(),
                usingDefault,
                rule != null ? rule.getUpdatedAt() : null,
                field.fullPrompt(rules));
    }
}
