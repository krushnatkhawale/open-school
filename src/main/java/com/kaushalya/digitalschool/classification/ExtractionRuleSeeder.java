package com.kaushalya.digitalschool.classification;

import com.kaushalya.digitalschool.storage.ExtractionRule;
import com.kaushalya.digitalschool.storage.ExtractionRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ExtractionRuleSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExtractionRuleSeeder.class);

    private final ExtractionRuleRepository repository;

    public ExtractionRuleSeeder(ExtractionRuleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (ExtractionField field : ExtractionField.values()) {
            if (repository.existsById(field.key())) {
                continue;
            }
            repository.save(new ExtractionRule(field.key(), field.defaultRules(), true, Instant.now()));
            log.info("Seeded default extraction rules for field '{}'", field.key());
        }
    }
}
