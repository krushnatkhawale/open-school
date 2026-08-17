package com.kaushalya.digitalschool.classification;

import com.kaushalya.digitalschool.shared.CommunicationType;
import com.kaushalya.digitalschool.shared.ScheduledEventData;

import java.util.List;

public interface AiChatService {
    String chat(String message, String userId);
    CommunicationType classify(String message);
    CommunicationType classify(String message, byte[] imageData, String mediaType);
    String describeImage(byte[] imageData, String mediaType);
    String extractDate(String text);
    List<ScheduledEventData> extractScheduledEvents(String text);
    List<String> extractTags(String text);
}
