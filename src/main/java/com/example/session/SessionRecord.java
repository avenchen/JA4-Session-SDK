package com.example.session;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class SessionRecord {
    private String sessionId;
    private String user;
    private String ja4Fingerprint;
    private String clientFingerprint;
    private String userAgent;
    private String ipAddress;
    private Map<String, String> clientSignals;
    private long createdAt;
    private long lastSeenAt;
    private SessionStatus status;

    public SessionRecord() {
    }

    public static SessionRecord create(String sessionId,
                                       String user,
                                       String ja4Fingerprint,
                                       String clientFingerprint,
                                       String userAgent,
                                       String ipAddress,
                                       Map<String, String> clientSignals) {
        SessionRecord record = new SessionRecord();
        record.sessionId = sessionId;
        record.user = user;
        record.ja4Fingerprint = ja4Fingerprint;
        record.clientFingerprint = clientFingerprint;
        record.userAgent = userAgent;
        record.ipAddress = ipAddress;
        record.clientSignals = clientSignals;
        Instant now = Instant.now();
        record.createdAt = now.toEpochMilli();
        record.lastSeenAt = now.toEpochMilli();
        record.status = SessionStatus.ACTIVE;
        return record;
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

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Map<String, String> getClientSignals() {
        return clientSignals;
    }

    public Instant getCreatedAt() {
        return Instant.ofEpochMilli(createdAt);
    }

    public Instant getLastSeenAt() {
        return Instant.ofEpochMilli(lastSeenAt);
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public void touch(Instant moment, String currentIp, String currentUserAgent) {
        this.lastSeenAt = moment.toEpochMilli();
        if (currentIp != null) {
            this.ipAddress = currentIp;
        }
        if (currentUserAgent != null) {
            this.userAgent = currentUserAgent;
        }
    }

    public boolean isDifferentUserAgent(String candidate) {
        return candidate != null && !Objects.equals(userAgent, candidate);
    }

    public boolean isDifferentIp(String candidate) {
        return candidate != null && !Objects.equals(ipAddress, candidate);
    }
}
