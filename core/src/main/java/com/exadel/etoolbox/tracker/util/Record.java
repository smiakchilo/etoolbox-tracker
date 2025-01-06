package com.exadel.etoolbox.tracker.util;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.math.util.MathUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class Record {
    private static final int MICROSECONDS_TO_MILLIS = 1000;

    private static final String APOSTROPHE = "'";
    private static final String PIPE = "|";

    private static final Pattern BRACKETED_LABEL_PATTERN = Pattern.compile("^\\w+\\(.*?\\)$");
    private static final Pattern PREFIXED_LABEL_PATTERN = Pattern.compile("^\\[\\w+]");

    private String category;
    private String label;
    private double time;
    private List<Record> records;
    private String comment;

    @Getter
    private boolean skipped;

    Record(String message) {
        label = StringUtils.substringBetween(message, TrackWriter.CURLY_BRACKET, TrackWriter.CLOSING_CURLY_BRACKET);
    }

    void append(Record value) {
        if (records == null) {
            records = new ArrayList<>();
        }
        records.add(value);
    }

    boolean isMatch(String message) {
        return StringUtils.contains(message, TrackWriter.COMMA + label + TrackWriter.CLOSING_CURLY_BRACKET);
    }

    void completeWith(String message) {
        String content = StringUtils.substringBetween(message, TrackWriter.CURLY_BRACKET,
                TrackWriter.CLOSING_CURLY_BRACKET);
        String timing = StringUtils.substringBefore(content, TrackWriter.COMMA);
        if (NumberUtils.isParsable(timing)) {
            this.time = MathUtils.round(Integer.parseInt(timing) / (MICROSECONDS_TO_MILLIS * 1.0d), 1);
        }
        this.comment = StringUtils.trim(StringUtils.substringAfter(message, TrackWriter.CLOSING_CURLY_BRACKET));
        if (TrackWriter.SKIP.equals(this.comment)) {
            this.comment = null;
            this.skipped = true;
        }
        extractCategory();
        prepareLabel();
    }

    private void extractCategory() {
        if (BRACKETED_LABEL_PATTERN.matcher(label).matches()) {
            String prefix = StringUtils.substringBefore(label, TrackWriter.ROUND_BRACKET);
            label = StringUtils.strip(
                    label.substring(prefix.length()),
                    TrackWriter.ROUND_BRACKET + TrackWriter.CLOSING_ROUND_BRACKET + StringUtils.SPACE);
            if (StringUtils.contains(prefix, "Resource")) {
                category = "resolve-resource";
            } else if (StringUtils.contains(prefix, "Servlet")) {
                category = "resolve-servlet";
            } else {
                label = prefix + StringUtils.SPACE + label;
            }
        } else if ("Request Processing".equals(label)) {
            category = "root";
        } else if (StringUtils.startsWithAny(label, "/apps/", "/libs/")) {
            category = "component";
        } else if (PREFIXED_LABEL_PATTERN.matcher(label).find()) {
            category = StringUtils.substringBetween(label, TrackWriter.SQUARE_BRACKET, TrackWriter.CLOSING_SQUARE_BRACKET);
            label = StringUtils.substringAfter(label, TrackWriter.CLOSING_SQUARE_BRACKET).trim();
        }
    }

    private void prepareLabel() {
        label = label
                .replace(TrackWriter.QUOTATION_MARK, APOSTROPHE)
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        if (label.contains(PIPE)) {
            comment = StringUtils.substringAfter(label, PIPE).trim();
            label = StringUtils.substringBefore(label, PIPE).trim();
        }
    }

    /* -------------
       Serialization
       ------------- */

    @Override
    public String toString() {
        return label;
    }

    String toJson() {
        StringBuilder builder = new StringBuilder("{");
        if (StringUtils.isNotBlank(category)) {
            builder.append("\"category\":\"").append(category).append(TrackWriter.QUOTATION_MARK).append(TrackWriter.COMMA);
        }
        builder
                .append("\"label\":\"").append(label).append(TrackWriter.QUOTATION_MARK).append(TrackWriter.COMMA)
                .append("\"time\":").append(String.format("%.1f", getTime())).append(TrackWriter.COMMA)
                .append("\"ownTime\":").append(String.format("%.1f", getOwnTime()));
        if (StringUtils.isNotEmpty(comment)) {
            builder.append(TrackWriter.COMMA).append("\"comment\":\"").append(comment).append(TrackWriter.QUOTATION_MARK);
        }
        if (CollectionUtils.isNotEmpty(records)) {
            builder
                    .append(TrackWriter.COMMA)
                    .append("\"records\":")
                    .append(TrackWriter.SQUARE_BRACKET);
            boolean leadingComma = false;
            for (Record child : records) {
                builder.append(leadingComma ? TrackWriter.COMMA : StringUtils.EMPTY);
                leadingComma = true;
                builder.append(child.toJson());
            }
            builder.append(TrackWriter.CLOSING_SQUARE_BRACKET);
        }
        return builder.append(TrackWriter.CLOSING_CURLY_BRACKET).toString();
    }

    private double getTime() {
        double result = time;
        if (CollectionUtils.isNotEmpty(records)) {
            // There can be a situation when the "parent" time is smaller than the sum of the "children" times due
            // to an unclosed parent record
            result = Math.max(result, records.stream().mapToDouble(Record::getTime).sum());
        }
        return result;
    }

    private double getOwnTime() {
        if (CollectionUtils.isEmpty(records)) {
            return getTime();
        }
        return getTime() - records.stream().mapToDouble(Record::getTime).sum();
    }
}
