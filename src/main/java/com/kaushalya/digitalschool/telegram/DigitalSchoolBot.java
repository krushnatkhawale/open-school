package com.kaushalya.digitalschool.telegram;

import com.kaushalya.digitalschool.classification.AiChatService;
import com.kaushalya.digitalschool.onboarding.OnboardingOutcome;
import com.kaushalya.digitalschool.onboarding.OnboardingService;
import com.kaushalya.digitalschool.shared.ClassificationEvent;
import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ScheduledEventData;
import com.kaushalya.digitalschool.shared.ScheduledEventType;
import com.kaushalya.digitalschool.storage.ScheduledEvent;
import com.kaushalya.digitalschool.storage.ScheduledEventRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.photo.PhotoSize;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Component
public class DigitalSchoolBot extends DefaultLongPollingUpdateConsumer implements SpringLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(DigitalSchoolBot.class);

    private final String botToken;
    private final TelegramClient telegramClient;
    private final AiChatService aiChatService;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledEventRepository scheduledEventRepository;
    private final OnboardingService onboardingService;

    public DigitalSchoolBot(@Value("${telegram.bot-token}") String botToken,
                            AiChatService aiChatService,
                            ApplicationEventPublisher eventPublisher,
                            ScheduledEventRepository scheduledEventRepository,
                            OnboardingService onboardingService) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.aiChatService = aiChatService;
        this.eventPublisher = eventPublisher;
        this.scheduledEventRepository = scheduledEventRepository;
        this.onboardingService = onboardingService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    @Transactional
    public void consume(Update update) {
        if (!update.hasMessage()) {
            log.debug("Received update without message, skipping");
            return;
        }
        var message = update.getMessage();

        if (message.hasText() && message.hasPhoto()) {
            handlePhotoMessage(update);
        } else if (message.hasText()) {
            handleTextMessage(update);
        } else if (message.hasPhoto()) {
            handlePhotoMessage(update);
        } else if (message.hasDocument()) {
            handleDocumentMessage(update);
        } else if (message.hasVideo() || message.hasVideoNote()) {
            handleUnsupportedMedia(update, "video");
        } else if (message.hasVoice() || message.hasAudio()) {
            handleUnsupportedMedia(update, "audio");
        }
    }

    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        String userDisplay = getUserDisplay(update);

        log.info("Text message from {} (chat {}): {}", userDisplay, chatId, messageText);

        try {
            var type = aiChatService.classify(messageText);
            log.info("Classification result for text from chat {}: {}", chatId, type);
            Instant now = Instant.now();
            String dateStr = aiChatService.extractDate(messageText);
            LocalDate eventDate = dateStr != null ? LocalDate.parse(dateStr) : null;
            List<String> tags = aiChatService.extractTags(messageText);
            boolean feedHidden = onboardingService.isOnboarding(chatId);
            var event = new ClassificationEvent(chatId, messageText, type, null, eventDate, now, null, null, tags, feedHidden);
            eventPublisher.publishEvent(event);
            log.info("Published ClassificationEvent: chatId={} type={} eventDate={} tags={} feedHidden={}", event.chatId(), event.type(), event.eventDate(), event.tags(), event.feedHidden());
            List<ScheduledEventData> plannedEvents = aiChatService.extractScheduledEvents(messageText);
            persistScheduledEvents(chatId, messageText, plannedEvents);
            sendMessage(chatId, onboardingReply(chatId, messageText, type.name()));
        } catch (Exception e) {
            log.error("Classification failed for chat {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Sorry, I'm having trouble processing that right now.");
        }
    }

    private void handlePhotoMessage(Update update) {
        String caption = update.getMessage().getCaption();
        Long chatId = update.getMessage().getChatId();
        String userDisplay = getUserDisplay(update);

        log.info("Photo from {} (chat {}): caption='{}'", userDisplay, chatId, caption);

        try {
            PhotoSize largestPhoto = largestPhoto(update.getMessage().getPhoto());

            byte[] imageData = downloadFile(largestPhoto.getFileId());

            String description = aiChatService.describeImage(imageData, "image/jpeg");
            boolean hasCaption = caption != null && !caption.isBlank();
            String textToClassify = hasCaption ? caption + "\n" + description : description;
            String storedText = storedTextFor(caption, description);
            log.info("Media text for classification: {}", textToClassify);
            var type = aiChatService.classify(textToClassify);
            log.info("Classification result for photo from chat {}: {}", chatId, type);
            Instant now = Instant.now();
            String dateStr = aiChatService.extractDate(textToClassify);
            LocalDate eventDate = dateStr != null ? LocalDate.parse(dateStr) : null;
            List<String> tags = aiChatService.extractTags(textToClassify);
            boolean feedHidden = onboardingService.isOnboarding(chatId);
            var event = new ClassificationEvent(chatId, storedText, type, description, eventDate, now, imageData, "image/jpeg", tags, feedHidden);
            eventPublisher.publishEvent(event);
            log.info("Published ClassificationEvent with aiDescription: chatId={} type={} eventDate={} tags={} feedHidden={}", event.chatId(), event.type(), event.eventDate(), event.tags(), event.feedHidden());
            List<ScheduledEventData> plannedEvents = aiChatService.extractScheduledEvents(textToClassify);
            persistScheduledEvents(chatId, textToClassify, plannedEvents);
            sendMessage(chatId, onboardingReply(chatId, null, type.name()));
        } catch (Exception e) {
            log.error("Photo processing failed for chat {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Sorry, I'm having trouble processing that right now.");
        }
    }

    private void handleDocumentMessage(Update update) {
        var document = update.getMessage().getDocument();
        Long chatId = update.getMessage().getChatId();
        String userDisplay = getUserDisplay(update);
        String mimeType = document.getMimeType();
        String fileName = document.getFileName();

        log.info("Document from {} (chat {}): mimeType='{}' fileName='{}'", userDisplay, chatId, mimeType, fileName);

        if (!"application/pdf".equals(mimeType)) {
            log.info("Unsupported document type: {}", mimeType);
            sendMessage(chatId, "I can only process PDF documents at the moment. Please send a PDF file.");
            return;
        }

        try {
            byte[] fileData = downloadFile(document.getFileId());

            String text;
            try (PDDocument pdf = Loader.loadPDF(fileData)) {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(pdf);
            }

            if (text == null || text.isBlank()) {
                log.warn("No text could be extracted from PDF");
                sendMessage(chatId, "I could not extract any readable text from this PDF. It may be a scanned document, which I cannot process yet.");
                return;
            }

            log.info("Extracted {} characters from PDF '{}': {}", text.length(), fileName, text);
            var type = aiChatService.classify(text);
            log.info("Classification result for PDF from chat {}: {}", chatId, type);
            Instant now = Instant.now();
            String dateStr = aiChatService.extractDate(text);
            LocalDate eventDate = dateStr != null ? LocalDate.parse(dateStr) : null;
            List<String> tags = aiChatService.extractTags(text);
            boolean feedHidden = onboardingService.isOnboarding(chatId);
            var event = new ClassificationEvent(chatId, text, type, null, eventDate, now, null, null, tags, feedHidden);
            eventPublisher.publishEvent(event);
            log.info("Published ClassificationEvent for PDF: chatId={} type={} eventDate={} tags={} feedHidden={}", event.chatId(), event.type(), event.eventDate(), event.tags(), event.feedHidden());
            List<ScheduledEventData> plannedEvents = aiChatService.extractScheduledEvents(text);
            persistScheduledEvents(chatId, text, plannedEvents);
            sendMessage(chatId, onboardingReply(chatId, null, type.name()));
        } catch (Exception e) {
            log.error("PDF processing failed for chat {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Sorry, I had trouble reading this PDF. If it's a scanned document, I cannot process images in PDFs yet.");
        }
    }

    static PhotoSize largestPhoto(List<PhotoSize> photos) {
        return photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElseThrow(() -> new NoSuchElementException("No photo sizes available"));
    }

    static String storedTextFor(String caption, String description) {
        return (caption != null && !caption.isBlank()) ? caption : description;
    }

    private void persistScheduledEvents(Long chatId, String sourceText, List<ScheduledEventData> events) {
        if (events == null || events.isEmpty()) return;
        Instant now = Instant.now();
        for (ScheduledEventData data : events) {
            try {
                ScheduledEventType type = ScheduledEventType.valueOf(data.type().toUpperCase());
                ScheduledEvent event = new ScheduledEvent(UUID.randomUUID(), data.date(),
                        data.title(), type, chatId, sourceText, now);
                scheduledEventRepository.save(event);
                log.info("Saved scheduled event: {} on {} for chat {}", data.title(), data.date(), chatId);
            } catch (Exception e) {
                log.warn("Failed to save scheduled event '{}': {}", data.title(), e.getMessage());
            }
        }
    }

    private void handleUnsupportedMedia(Update update, String mediaType) {
        Long chatId = update.getMessage().getChatId();
        String userDisplay = getUserDisplay(update);
        log.info("Unsupported {} from {} (chat {})", mediaType, userDisplay, chatId);
        sendMessage(chatId, "I'm sorry, I cannot process " + mediaType + " messages yet. Please send text or images.");
    }

    private byte[] downloadFile(String fileId) throws TelegramApiException, IOException {
        GetFile getFile = GetFile.builder().fileId(fileId).build();
        org.telegram.telegrambots.meta.api.objects.File file = telegramClient.execute(getFile);
        try (InputStream stream = telegramClient.downloadFileAsStream(file)) {
            return stream.readAllBytes();
        }
    }

    private String getUserDisplay(Update update) {
        String userName = update.getMessage().getFrom().getUserName();
        Long chatId = update.getMessage().getChatId();
        return userName != null ? userName : "user_" + chatId;
    }

    String onboardingReply(Long chatId, String textOrNull, String classificationReply) {
        OnboardingOutcome outcome = onboardingService.handle(chatId, textOrNull);
        if (outcome.newUser()) {
            return classificationReply + "\n\n" + outcome.text();
        }
        if (outcome.pending()) {
            return outcome.text();
        }
        return classificationReply;
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }
}
