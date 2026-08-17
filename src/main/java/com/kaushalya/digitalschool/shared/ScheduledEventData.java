package com.kaushalya.digitalschool.shared;

import java.time.LocalDate;

public record ScheduledEventData(LocalDate date, String type, String title) {
}
