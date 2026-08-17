package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ClassificationUpdateRequest(
        @NotNull CommunicationType communicationType,
        String originalText,
        @NotNull LocalDate eventDate,
        String tags) {
}
