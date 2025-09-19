package com.example.session;

import java.time.Instant;
import java.util.Map;

public class RiskEvent {
    private RiskEventType type;
    private String sessionId;
    private String user;
    private String ja4Fingerprint;
    private String clientFingerprint;
    private String ipAddress;
    private String userAgent;
    private String message;
    private Map<String, String> details;
    private long timestamp;

    public RiskEvent() {
    }

    public static RiskEvent create(RiskEventType type,
                                   SessionRecord record,
                                   String ip,
                                   String userAgent,
                                   String message,
                                   Map<String, String> details) {
        RiskEvent event = new RiskEvent();
        event.type = type;
        if (record != null) {
            event.sessionId = record.getSessionId();
            event.user = record.getUser();
            event.ja4Fingerprint = record.getJa4Fingerprint();
            event.clientFingerprint = record.getClientFingerprint();
        }
        event.ipAddress = ip;
        event.userAgent = userAgent;
        event.message = message;
        event.details = details;
        event.timestamp = Instant.now().toEpochMilli();
        return event;
    }

    public RiskEventType getType() {
        return type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUser() {
        return user;
    }

    public String getJa4Fingerprint() {
        return ja4Fingerprint;
    }

    public String getClientFingerprint() {
        return clientFingerprint;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return Instant.ofEpochMilli(timestamp);
    }
}
