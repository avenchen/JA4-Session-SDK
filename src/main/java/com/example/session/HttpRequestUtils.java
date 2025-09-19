package com.example.session;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class HttpRequestUtils {
    private static final String HEADER_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_REAL_IP = "X-Real-IP";

    private HttpRequestUtils() {
    }

    public static String resolveClientIp(HttpServletRequest request) {
        return Optional.ofNullable(extractHeader(request, HEADER_FORWARDED_FOR))
                .map(value -> value.split(",", 2)[0].trim())
                .filter(ip -> !ip.isEmpty())
                .or(() -> Optional.ofNullable(extractHeader(request, HEADER_REAL_IP)))
                .filter(ip -> !ip.isEmpty())
                .orElseGet(request::getRemoteAddr);
    }

    public static String resolveUserAgent(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown-agent");
    }

    private static String extractHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value == null ? null : value.trim();
    }
}
