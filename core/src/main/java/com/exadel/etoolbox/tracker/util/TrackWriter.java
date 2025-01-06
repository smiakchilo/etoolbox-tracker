package com.exadel.etoolbox.tracker.util;

import com.exadel.etoolbox.tracker.service.RegistrationStatus;
import com.exadel.etoolbox.tracker.servlet.TrackServlet;
import lombok.Builder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Builder
public class TrackWriter {

    public static final String SKIP = "skip";

    static final String COMMA = ",";
    static final String QUOTATION_MARK = "\"";

    static final String CURLY_BRACKET = "{";
    static final String CLOSING_CURLY_BRACKET = "}";
    static final String SQUARE_BRACKET = "[";
    static final String CLOSING_SQUARE_BRACKET = "]";
    static final String ROUND_BRACKET = "(";
    static final String CLOSING_ROUND_BRACKET = ")";

    private SlingHttpServletRequest request;
    private int status;
    private RegistrationStatus hooksStatus;

    public void writeTo(SlingHttpServletResponse response) throws IOException {
        Iterator<String> messages = request.getRequestProgressTracker().getMessages();
        if (messages == null) {
            response.getWriter().println(CURLY_BRACKET + CLOSING_CURLY_BRACKET);
            response.getWriter().flush();
            return;
        }
        LinkedList<Record> recordStack = new LinkedList<>();
        while (messages.hasNext()) {
            String message = messages.next();
            if (isStartingEntry(message) && !isSkippedEntry(message)) {
                Record newRecord = new Record(message);
                recordStack.add(newRecord);
                continue;
            }
            if (!isEndingEntry(message) || isSkippedEntry(message)) {
                continue;
            }
            Record match = StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(recordStack.descendingIterator(),0), false)
                    .filter(record -> record.isMatch(message))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                match.completeWith(message);
                if (match.isSkipped()) {
                    recordStack.remove(match);
                    continue;
                }
                // There can be "unclosed" frames up the stack that we close manually before wrapping up the match itself
                boolean matchHit = false;
                while (!matchHit) {
                    Record last = recordStack.removeLast();
                    recordStack.getLast().append(last);
                    if (last == match) {
                        matchHit = true;
                    }
                }
            }
        }
        response.getWriter().write(toJson(recordStack));
        response.getWriter().flush();
    }

    /* -------------
       Serialization
       ------------- */

    private String toJson(List<Record> records) {
        StringBuilder builder = new StringBuilder(CURLY_BRACKET);
        builder.append("\"status\":").append(status).append(COMMA);
        if (CollectionUtils.isNotEmpty(records)) {
            builder.append("\"records\":").append(SQUARE_BRACKET);
            boolean leadingComma = false;
            for (Record record : records) {
                builder.append(leadingComma ? COMMA : StringUtils.EMPTY);
                leadingComma = true;
                builder.append(record.toJson());
            }
            builder.append(CLOSING_SQUARE_BRACKET);
        }
        builder.append(COMMA).append("\"url\":\"").append(getDetailsUrl(request)).append(QUOTATION_MARK);
        builder.append(COMMA).append("\"hooks\":\"").append(hooksStatus.toString()).append(QUOTATION_MARK);
        builder.append(CLOSING_CURLY_BRACKET);
        return builder.toString();
    }

    private static String getDetailsUrl(SlingHttpServletRequest request) {
        return "//"
                + request.getServerName()
                + ":" + request.getServerPort()
                + request.getRequestURI().replace("track.json", "etoolbox-tracker/console.html");
    }

    /* ------------------
       Collecting records
       ------------------ */

    private static boolean isStartingEntry(String value) {
        return Stream.of("TIMER_START", CURLY_BRACKET, CLOSING_CURLY_BRACKET).allMatch(value::contains);
    }

    private static boolean isEndingEntry(String value) {
        return Stream.of("TIMER_END", CURLY_BRACKET, CLOSING_CURLY_BRACKET).allMatch(value::contains);
    }

    private static boolean isSkippedEntry(String value) {
        if (StringUtils.isEmpty(value)) {
            return true;
        }
        return Stream.of(
                        TrackServlet.class.getName(),
                        ROUND_BRACKET + TrackServlet.PATH + CLOSING_ROUND_BRACKET,
                        "{ServletResolution}",
                        ",ServletResolution}" ,
                        "{ResourceResolution}",
                        ",ResourceResolution}")
                .anyMatch(value::contains);
    }
}
