package com.kaushalya.digitalschool.shared;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "school")
public record SchoolProperties(@DefaultValue("4") int academicYearStartMonth) {
}
