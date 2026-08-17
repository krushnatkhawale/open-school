package com.kaushalya.digitalschool.web;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FeedSearch {

    public String highlight(String text, String searchTerm) {
        if (text == null) {
            return null;
        }
        if (searchTerm == null) {
            return escapeHtml(text);
        }
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("(?i)(" + Pattern.quote(searchTerm) + ")");
        Matcher matcher = pattern.matcher(text);
        int last = 0;
        while (matcher.find()) {
            result.append(escapeHtml(text.substring(last, matcher.start())));
            result.append("<mark>").append(escapeHtml(matcher.group(1))).append("</mark>");
            last = matcher.end();
        }
        result.append(escapeHtml(text.substring(last)));
        return result.toString();
    }

    static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
