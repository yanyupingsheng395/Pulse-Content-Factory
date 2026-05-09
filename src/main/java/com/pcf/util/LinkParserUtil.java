package com.pcf.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LinkParserUtil {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s\\]\\)\"'<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    private LinkParserUtil() {
    }

    public static List<String> extractUrls(String text) {
        if (StringUtil.isBlank(text)) {
            return Collections.emptyList();
        }
        Set<String> ordered = new LinkedHashSet<>();
        Matcher m = URL_PATTERN.matcher(text);
        while (m.find()) {
            String raw = trimTrailingPunctuation(m.group(1));
            if (looksLikeHttpUrl(raw)) {
                ordered.add(raw);
            }
        }
        return new ArrayList<>(ordered);
    }

    public static String firstUrlOrNull(String text) {
        List<String> urls = extractUrls(text);
        return urls.isEmpty() ? null : urls.get(0);
    }

    private static String trimTrailingPunctuation(String u) {
        return u.replaceAll("[),.;!?]+$", "");
    }

    private static boolean looksLikeHttpUrl(String candidate) {
        try {
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }
}
