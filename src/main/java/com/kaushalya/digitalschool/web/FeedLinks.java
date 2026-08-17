package com.kaushalya.digitalschool.web;

import com.kaushalya.digitalschool.shared.CommunicationType;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Component
public class FeedLinks {

    public String tagUrl(String tag, String q, List<CommunicationType> types, Long chatId, UUID schoolId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/feed");
        addParam(builder, "tag", tag);
        addParam(builder, "q", q);
        addTypes(builder, types);
        addParam(builder, "chatId", chatId);
        addParam(builder, "schoolId", schoolId);
        return builder.toUriString();
    }

    public String removeTagUrl(String q, List<CommunicationType> types, Long chatId, UUID schoolId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/feed");
        addParam(builder, "q", q);
        addTypes(builder, types);
        addParam(builder, "chatId", chatId);
        addParam(builder, "schoolId", schoolId);
        return builder.toUriString();
    }

    public String clearSearchUrl(List<CommunicationType> types, Long chatId, String tag, UUID schoolId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/feed");
        addTypes(builder, types);
        addParam(builder, "chatId", chatId);
        addParam(builder, "tag", tag);
        addParam(builder, "schoolId", schoolId);
        return builder.toUriString();
    }

    private static void addTypes(UriComponentsBuilder builder, List<CommunicationType> types) {
        if (types != null) {
            for (CommunicationType t : types) {
                builder.queryParam("types", t.name());
            }
        }
    }

    private static void addParam(UriComponentsBuilder builder, String name, Object value) {
        if (value != null) {
            builder.queryParam(name, value);
        }
    }
}
