package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/classifications")
public class ClassificationController {

    private final ClassificationRecordRepository repository;

    public ClassificationController(ClassificationRecordRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<ClassificationResponse> getAll() {
        return repository.findAllByFeedHiddenFalseOrderByUploadedAtDesc().stream()
                .map(ClassificationResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassificationResponse> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(r -> ResponseEntity.ok(ClassificationResponse.from(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(params = "chatId")
    public List<ClassificationResponse> getByChatId(@RequestParam Long chatId) {
        return repository.findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(chatId).stream()
                .map(ClassificationResponse::from)
                .toList();
    }

    @GetMapping(params = "type")
    public List<ClassificationResponse> getByType(@RequestParam CommunicationType type) {
        return repository.findByCommunicationTypeAndFeedHiddenFalseOrderByUploadedAtDesc(type).stream()
                .map(ClassificationResponse::from)
                .toList();
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID id) {
        return repository.findById(id)
                .filter(r -> r.getImageData() != null)
                .map(r -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(
                                r.getImageContentType() != null ? r.getImageContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=31536000")
                        .body(r.getImageData()))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClassificationResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody ClassificationUpdateRequest update) {
        return repository.findById(id)
                .map(record -> {
                    record.setCommunicationType(update.communicationType());
                    record.setOriginalText(normalizeNullable(update.originalText()));
                    record.setEventDate(update.eventDate());
                    record.setTags(normalizeTags(update.tags()));
                    return ResponseEntity.ok(ClassificationResponse.from(repository.save(record)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private static String normalizeNullable(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private static String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return null;
        }
        String normalized = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        return normalized.isEmpty() ? null : normalized;
    }
}
