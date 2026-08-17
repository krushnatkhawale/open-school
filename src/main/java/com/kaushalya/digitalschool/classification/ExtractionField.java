package com.kaushalya.digitalschool.classification;

import java.util.Arrays;

public enum ExtractionField {

    CLASSIFICATION(
            "classification",
            "Classification",
            "Category of the school message: daily lesson update, reminder, action required, etc.",
            """
                    You are a school communication classifier. Your task is to classify school messages into exactly one category.

                    Categories:
                    - DAILY_LESSON_UPDATE: What was taught in class — subjects covered, learning objectives for the day. Often shared as a photo of a notebook or textbook page with a visible date (handwritten or printed), along with student work like circled numbers, crossed-out answers, tick marks, and corrections.
                    - GENERAL_INFORMATION: General information or notices for parents (announcements, policy changes, homework reminders, attendance notes, achievements — anything informational parents should be aware of with no specific date or action)
                    - REMINDER: Date-specific items parents should mark on a calendar (exam schedules, event dates, holidays, parent-teacher meetings, field trips, submission deadlines)
                    - ACTION_REQUIRED: Parent must take action (sign a form, attend a meeting, submit documents, pay a fee, reply to the school)
                    - OTHER_INFORMATION: Content that appears to be school-related but does not clearly fit into the above categories
                    """,
            "Respond with ONLY the category name (e.g., DAILY_LESSON_UPDATE). Do not include any explanation or extra text.",
            null),

    IMAGE_DESCRIPTION(
            "imageDescription",
            "Image description",
            "Free-text description of what an image shows (used for photos of notebook pages, notices, etc.).",
            """
                    For notebook or textbook pages (daily lesson updates), list every date you see: its exact position on the page (top margin, header, bottom, footer), its ink color (pencil, red, blue, black), and whether it is accompanied by a signature or stamp. A date at the bottom in red ink is the teacher's date — report it explicitly even if other dates differ. If the same date appears in different places, mention that too.
                    """,
            "You are a helpful assistant. Describe what is shown in the image concisely.",
            "Describe everything visible in this image."),

    DATE(
            "date",
            "Lesson date",
            "The lesson date in YYYY-MM-DD, extracted from text or image descriptions (teacher's red bottom date preferred).",
            """
                    Extract the lesson date from this school communication text. Daily lesson updates are usually photos of textbook or notebook pages. Such pages typically have two dates:
                    1) a date near the top or in the top margin written in pencil by the student — it may be absent, faint, or incorrect;
                    2) a date near the bottom in red ink, often beside the teacher's signature or stamp — written by the teacher and the most reliable.
                    Prefer the teacher's red bottom date as the lesson date. Only if that red bottom date is absent, fall back to the pencil date at the top.
                    """,
            "Return ONLY the date in YYYY-MM-DD format. If no date is found, return NONE. Do not include any explanation or extra text.",
            null),

    SCHEDULED_EVENTS(
            "scheduledEvents",
            "Scheduled events",
            "Planned events (holidays, exams, meetings, trips, submission deadlines) extracted as a JSON array.",
            "You are a school calendar parser. Extract all planned events (holidays, exams, parent-teacher meetings, field trips, submission deadlines) from this text.",
            "Return a JSON array of objects with fields: date (YYYY-MM-DD), type (HOLIDAY|EXAM|PARENT_TEACHER_MEETING|FIELD_TRIP|SUBMISSION_DEADLINE|OTHER), and title (short description). Return ONLY the JSON array, no explanation. If no events are found, return [].",
            null),

    TAGS(
            "tags",
            "Tags",
            "Short reusable tags for content or language (e.g. maths, hindi, homework).",
            "You are a school content tagger. Extract short, reusable tags from this school communication (message text and/or image description). Tags describe content or language, e.g. story, poem, maths, science, hindi, english, reading, homework, exam, activity, notice, sports.",
            "Return ONLY a comma-separated list of lowercase tags. If no tags apply, return NONE. Do not include any explanation or extra text.",
            null);

    private final String key;
    private final String label;
    private final String description;
    private final String defaultRules;
    private final String contract;
    private final String userInstruction;

    ExtractionField(String key, String label, String description,
                    String defaultRules, String contract, String userInstruction) {
        this.key = key;
        this.label = label;
        this.description = description;
        this.defaultRules = defaultRules;
        this.contract = contract;
        this.userInstruction = userInstruction;
    }

    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String defaultRules() {
        return defaultRules;
    }

    public String contract() {
        return contract;
    }

    public String userInstruction() {
        return userInstruction;
    }

    public String prompt(String rules) {
        return switch (this) {
            case IMAGE_DESCRIPTION -> contract + "\n\n" + rules;
            default -> rules + "\n\n" + contract;
        };
    }

    public String fullPrompt(String rules) {
        if (userInstruction == null) {
            return prompt(rules);
        }
        return prompt(rules) + "\n\n" + userInstruction;
    }

    public static ExtractionField fromKey(String key) {
        return Arrays.stream(values())
                .filter(f -> f.key.equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown extraction field: " + key));
    }
}
