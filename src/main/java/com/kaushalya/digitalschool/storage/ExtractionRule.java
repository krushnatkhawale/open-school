package com.kaushalya.digitalschool.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "extraction_rule")
public class ExtractionRule {

    @Id
    @Column(name = "rule_key", length = 50)
    private String key;

    @Column(name = "rules", columnDefinition = "TEXT")
    private String rules;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public ExtractionRule() {
    }

    public ExtractionRule(String key, String rules, boolean enabled) {
        this(key, rules, enabled, Instant.now());
    }

    public ExtractionRule(String key, String rules, boolean enabled, Instant updatedAt) {
        this.key = key;
        this.rules = rules;
        this.enabled = enabled;
        this.updatedAt = updatedAt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
