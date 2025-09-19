package com.example.session;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ja4FingerprintExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(Ja4FingerprintExtractor.class);
    private static final String[] HEADER_CANDIDATES = {
            "X-JA4-Fingerprint",
            "X-JA4",
            "X-Client-JA4",
            "Ja4-Fingerprint"
    };

    private Ja4FingerprintExtractor() {
    }

    public static Optional<String> readHeaderFingerprint(HttpServletRequest request) {
        return Arrays.stream(HEADER_CANDIDATES)
                .map(request::getHeader)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst();
    }

    public static String extract(HttpServletRequest request) {
        Optional<String> provided = readHeaderFingerprint(request);
        if (provided.isPresent()) {
            return provided.get();
        }
        String fallback = computeFallbackFingerprint(request);
        LOGGER.debug("Falling back to locally-derived fingerprint {}", fallback);
        return fallback;
    }

    private static String computeFallbackFingerprint(HttpServletRequest request) {
        SSLSession session = (SSLSession) request.getAttribute("jakarta.servlet.request.ssl_session");
        if (session == null) {
            session = (SSLSession) request.getAttribute("javax.servlet.request.ssl_session");
        }

        String protocol = session != null ? session.getProtocol() : request.getProtocol();
        String cipherSuite = session != null ? session.getCipherSuite() : readCipherAttribute(request);
        String tlsSessionId = session != null ? bytesToHex(session.getId()) : "no-session";
        String remoteAddress = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(value -> value.split(",", 2)[0].trim())
                .filter(s -> !s.isEmpty())
                .orElseGet(request::getRemoteAddr);
        String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown-agent");

        String canonical = String.join("|",
                "proto=" + protocol,
                "cipher=" + cipherSuite,
                "tlsId=" + tlsSessionId,
                "addr=" + remoteAddress,
                "ua=" + userAgent
        );
        LOGGER.debug("Derived fallback canonical string for fingerprint: {}", canonical);
        return sha256Hex(canonical);
    }

    private static String readCipherAttribute(HttpServletRequest request) {
        Object cipher = request.getAttribute("jakarta.servlet.request.cipher_suite");
        if (cipher == null) {
            cipher = request.getAttribute("javax.servlet.request.cipher_suite");
        }
        return cipher != null ? cipher.toString() : "unknown-cipher";
    }

    private static String sha256Hex(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
