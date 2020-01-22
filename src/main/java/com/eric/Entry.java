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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;

public class Entry implements Comparable<Entry> {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = DATE_FORMAT + "'T'HHmmss";
    private static final String[] DATE_FORMATS = { TIME_FORMAT + "Z", TIME_FORMAT, DATE_FORMAT };

    private static Long parseDate(final String str, TimeZone tz, final String... parsePatterns) {
        for (String pattern : parsePatterns) {
            try {
                return FastDateFormat.getInstance(pattern, tz).parse(str).getTime();
            } catch (Exception ex) {
            }
        }

        return null;
    }

    public static Entry parse(File file, char tagSeparator, TimeZone tz, int baseDirLength) {
        Entry entry = new Entry();

        entry.file = file;
        entry.path = file.getAbsolutePath().substring(baseDirLength);
        entry.m = file.lastModified();
        entry.s = file.length();

        String title = trim(FilenameUtils.getBaseName(entry.path));
        String part = substringBefore(title, " ");
        entry.ts = parseDate(part, tz, DATE_FORMATS);

        if (entry.ts != null) {
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

    public static Charset CHARSET = Charset.defaultCharset();

    private transient File file;

    private String title;

    private String path;

    private String slug;

    private String[] tags;

    private String author;

    private Long ts;

    private long m;

    private long s;

    public String getContents() throws IOException {
        return FileUtils.readFileToString(file, CHARSET);
    }

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
        return ts == null ? m : ts;
    }

    @Override
    public int compareTo(Entry o) {
        return Long.compare(o.getTimestamp(), getTimestamp());
    }
}
