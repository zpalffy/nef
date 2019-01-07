package com.eric;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.replaceChars;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.StringUtils.trim;

import java.io.File;
import java.util.TimeZone;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

public class Entry implements Comparable<Entry> {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = DATE_FORMAT + "'T'HHmmss";
    private static final String[] DATE_FORMATS = { TIME_FORMAT + "Z", TIME_FORMAT, DATE_FORMAT };

    private static long parseDate(final String str, TimeZone tz, final String... parsePatterns) {
        for (String pattern : parsePatterns) {
            try {
                return FastDateFormat.getInstance(pattern, tz).parse(str).getTime();
            } catch (Exception ex) {
            }
        }

        return 0L;
    }

    public static Entry parse(File file, char tagSeparator, TimeZone tz, int baseDirLength) {
        Entry entry = new Entry();

        entry.path = file.getAbsolutePath().substring(baseDirLength);
        String title = trim(FilenameUtils.getBaseName(entry.path));

        String part = substringBefore(title, " ");
        if (!part.equals(title)) {
            // try to pull off the date:
            entry.ts = parseDate(part, tz, DATE_FORMATS);
        }

        if (entry.ts == 0) {
            entry.ts = file.lastModified(); // default to the last modified of the file
        } else {
            title = trim(substringAfter(title, " ")); // successfully pulled the time off
        }

        // try to get tags:
        part = substringBetween(title, "[", "]");
        if (isNotEmpty(part)) {
            entry.tags = StringUtils.split(part, tagSeparator);
            title = RegExUtils.removePattern(title, "\\[\\s*" + part + "\\s*\\]");
        }

        // try to get author:
        part = substringBetween(title, "(", ")");
        if (isNotEmpty(part)) {
            entry.author = trim(part);
            title = RegExUtils.removePattern(title, "\\(\\s*" + entry.author + "\\s*\\)");
        }

        title = trim(title);
        if (isEmpty(title)) {
            entry.slug = DateFormatUtils.format(entry.ts, DATE_FORMAT);
        } else {
            entry.slug = replaceChars(lowerCase(title), ' ', '-');
            entry.slug = RegExUtils.removePattern(entry.slug, "[\\p{Punct}&&[^-_]]");

            entry.title = title;
        }

        return entry;
    }

    private String title;

    private String path;

    private String slug;

    private String[] tags;

    private String author;

    private long ts;

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public String getPath() {
        return path;
    }

    public String getSlug() {
        return slug;
    }

    public String[] getTags() {
        return tags;
    }

    public String getAuthor() {
        return author;
    }

    public long getTimestamp() {
        return ts;
    }

    @Override
    public int compareTo(Entry o) {
        return Long.compare(o.ts, ts);
    }
}
