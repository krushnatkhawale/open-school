package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ContentType;
import com.kaushalya.digitalschool.storage.ClassificationRecord;
import com.kaushalya.digitalschool.storage.ClassificationRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassificationControllerTest {

    @Mock
    private ClassificationRecordRepository repository;

    @InjectMocks
    private ClassificationController controller;

    @Test
    void getImageReturnsStoredBytesWithContentType() {
        byte[] png = new byte[]{1, 2, 3, 4};
        UUID id = UUID.randomUUID();
        ClassificationRecord record = recordWithImage(id, png, "image/png");
        when(repository.findById(id)).thenReturn(Optional.of(record));

        ResponseEntity<byte[]> response = controller.getImage(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
        assertArrayEquals(png, response.getBody());
        assertEquals("private, max-age=31536000", response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void getImageFallsBackToOctetStreamWhenMimeTypeUnknown() {
        UUID id = UUID.randomUUID();
        ClassificationRecord record = recordWithImage(id, new byte[]{1, 2}, null);
        when(repository.findById(id)).thenReturn(Optional.of(record));

        ResponseEntity<byte[]> response = controller.getImage(id);

        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
    }

    @Test
    void getImageReturns404WhenRecordHasNoImage() {
        UUID id = UUID.randomUUID();
        ClassificationRecord record = new ClassificationRecord(id, 12345L, "text",
                CommunicationType.REMINDER, null, LocalDate.now(), Instant.now());
        when(repository.findById(id)).thenReturn(Optional.of(record));

        ResponseEntity<byte[]> response = controller.getImage(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.hasBody());
    }

    @Test
    void getImageReturns404ForUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<byte[]> response = controller.getImage(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void updatePersistsEditableFieldsAndPreservesOthers() {
        UUID id = UUID.randomUUID();
        byte[] image = new byte[]{1, 2, 3};
        Instant uploaded = Instant.parse("2026-07-29T10:00:00Z");
        ClassificationRecord record = new ClassificationRecord(id, 999L, "old text",
                CommunicationType.REMINDER, "old desc", LocalDate.of(2026, 7, 29), uploaded,
                image, "image/png");
        when(repository.findById(id)).thenReturn(Optional.of(record));
        when(repository.save(any(ClassificationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassificationUpdateRequest update = new ClassificationUpdateRequest(
                CommunicationType.EVENT, " new text ",
                LocalDate.of(2026, 8, 1), " maths , english , maths ");

        ResponseEntity<ClassificationResponse> response = controller.update(id, update);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(CommunicationType.EVENT, response.getBody().communicationType());
        assertEquals("new text", response.getBody().originalText());
        assertEquals("old desc", response.getBody().aiDescription());
        assertEquals(LocalDate.of(2026, 8, 1), response.getBody().eventDate());
        assertEquals("maths,english", response.getBody().tags());
        assertEquals(999L, response.getBody().chatId());
        assertEquals(uploaded, response.getBody().uploadedAt());
        assertEquals("image/png", response.getBody().imageContentType());
    }

    @Test
    void updateNormalizesBlankFieldsToNull() {
        UUID id = UUID.randomUUID();
        ClassificationRecord record = new ClassificationRecord(id, 1L, "text",
                CommunicationType.REMINDER, "desc", LocalDate.of(2026, 7, 29), Instant.now());
        record.setTags("old");
        when(repository.findById(id)).thenReturn(Optional.of(record));
        when(repository.save(any(ClassificationRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        ClassificationUpdateRequest update = new ClassificationUpdateRequest(
                CommunicationType.EVENT, "   ", LocalDate.of(2026, 8, 1), " , , ");

        ResponseEntity<ClassificationResponse> response = controller.update(id, update);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody().originalText());
        assertEquals("desc", response.getBody().aiDescription());
        assertNull(response.getBody().tags());
    }

    @Test
    void updateReturns404ForUnknownId() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<ClassificationResponse> response = controller.update(id, validUpdate());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void getAllUsesFeedHiddenFilteredQueryAndMapsToResponse() {
        ClassificationRecord record = new ClassificationRecord(UUID.randomUUID(), 1L, "text",
                CommunicationType.REMINDER, null, LocalDate.now(), Instant.now());
        when(repository.findAllByFeedHiddenFalseOrderByUploadedAtDesc()).thenReturn(List.of(record));

        List<ClassificationResponse> result = controller.getAll();

        assertEquals(List.of(ClassificationResponse.from(record)), result);
        verify(repository).findAllByFeedHiddenFalseOrderByUploadedAtDesc();
    }

    @Test
    void getByChatIdUsesFeedHiddenFilteredQueryAndMapsToResponse() {
        ClassificationRecord record = new ClassificationRecord(UUID.randomUUID(), 42L, "text",
                CommunicationType.REMINDER, null, LocalDate.now(), Instant.now());
        when(repository.findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(42L)).thenReturn(List.of(record));

        List<ClassificationResponse> result = controller.getByChatId(42L);

        assertEquals(List.of(ClassificationResponse.from(record)), result);
        verify(repository).findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(42L);
    }

    @Test
    void getByTypeUsesFeedHiddenFilteredQueryAndMapsToResponse() {
        ClassificationRecord record = new ClassificationRecord(UUID.randomUUID(), 1L, "text",
                CommunicationType.REMINDER, null, LocalDate.now(), Instant.now());
        when(repository.findByCommunicationTypeAndFeedHiddenFalseOrderByUploadedAtDesc(CommunicationType.REMINDER))
                .thenReturn(List.of(record));

        List<ClassificationResponse> result = controller.getByType(CommunicationType.REMINDER);

        assertEquals(List.of(ClassificationResponse.from(record)), result);
        verify(repository).findByCommunicationTypeAndFeedHiddenFalseOrderByUploadedAtDesc(CommunicationType.REMINDER);
    }

    @Test
    void getByChatIdReturnsEmptyListWhenOnlyHiddenRecordsExist() {
        when(repository.findByChatIdAndFeedHiddenFalseOrderByUploadedAtDesc(42L)).thenReturn(Collections.emptyList());

        List<ClassificationResponse> result = controller.getByChatId(42L);

        assertTrue(result.isEmpty());
    }

    @Test
    void fromMapsAllSerializedEntityFields() {
        byte[] image = new byte[]{1, 2, 3};
        Instant uploaded = Instant.parse("2026-07-29T10:00:00Z");
        ClassificationRecord record = new ClassificationRecord(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                999L, "text", CommunicationType.REMINDER, "desc", LocalDate.of(2026, 7, 29), uploaded,
                image, "image/png");
        record.setContentType(ContentType.IMAGE);
        record.setTags("a,b");
        record.setFeedHidden(true);

        ClassificationResponse response = ClassificationResponse.from(record);

        assertEquals(record.getId(), response.id());
        assertEquals(999L, response.chatId());
        assertEquals("text", response.originalText());
        assertEquals(CommunicationType.REMINDER, response.communicationType());
        assertEquals("desc", response.aiDescription());
        assertEquals("image/png", response.imageContentType());
        assertEquals("a,b", response.tags());
        assertEquals(ContentType.IMAGE, response.contentType());
        assertEquals(LocalDate.of(2026, 7, 29), response.eventDate());
        assertEquals(uploaded, response.uploadedAt());
        assertTrue(response.feedHidden());
    }

    private ClassificationUpdateRequest validUpdate() {
        return new ClassificationUpdateRequest(
                CommunicationType.EVENT, "text", LocalDate.now(), null);
    }

    private ClassificationRecord recordWithImage(UUID id, byte[] imageData, String imageContentType) {
        return new ClassificationRecord(id, 12345L, "text",
                CommunicationType.REMINDER, "desc", LocalDate.now(), Instant.now(),
                imageData, imageContentType);
    }
}
