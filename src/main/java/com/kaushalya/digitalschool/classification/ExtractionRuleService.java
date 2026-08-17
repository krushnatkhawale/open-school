package com.kaushalya.digitalschool.classification;

import java.util.List;

public interface ExtractionRuleService {

    List<ExtractionRuleView> findAll();

    ExtractionRuleView findByKey(String key);

    ExtractionRuleView save(String key, String rules, boolean enabled);

    ExtractionRuleView reset(String key);

    String systemPromptFor(ExtractionField field);
}
