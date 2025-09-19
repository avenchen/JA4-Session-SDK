package com.example.session;

import com.google.gson.Gson;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

public final class SessionSecurityRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionSecurityRepository.class);
    private static final Gson GSON = new Gson();
    private static final String SESSION_KEY_PREFIX = "ja4:session:";
    private static final String RISK_EVENTS_KEY = "ja4:risk-events";
    private static final int SESSION_TTL_SECONDS = 3600;
    private static final int EVENTS_HISTORY_LIMIT = 200;

    private SessionSecurityRepository() {
    }

    private static String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    public static void persistSession(SessionRecord record) {
        if (!RedisManager.isEnabled() || record == null) {
            return;
        }
        RedisManager.execute(jedis -> {
            jedis.setex(sessionKey(record.getSessionId()), SESSION_TTL_SECONDS, GSON.toJson(record));
            return null;
        });
    }

    public static Optional<SessionRecord> getSession(String sessionId) {
        if (!RedisManager.isEnabled() || sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(RedisManager.execute(jedis -> readSession(jedis, sessionId)));
    }

    private static SessionRecord readSession(Jedis jedis, String sessionId) {
        String payload = jedis.get(sessionKey(sessionId));
        if (payload == null) {
            return null;
        }
        try {
            SessionRecord record = GSON.fromJson(payload, SessionRecord.class);
            return record;
        } catch (Exception ex) {
            LOGGER.warn("Failed to deserialize session record {}: {}", sessionId, ex.getMessage());
            return null;
        }
    }

    public static void refreshSession(SessionRecord record, String currentIp, String currentUserAgent) {
        if (!RedisManager.isEnabled() || record == null) {
            return;
        }
        record.touch(Instant.now(), currentIp, currentUserAgent);
        RedisManager.execute(jedis -> {
            jedis.setex(sessionKey(record.getSessionId()), SESSION_TTL_SECONDS, GSON.toJson(record));
            return null;
        });
    }

    public static void deleteSession(String sessionId) {
        if (!RedisManager.isEnabled() || sessionId == null) {
            return;
        }
        RedisManager.execute(jedis -> {
            jedis.del(sessionKey(sessionId));
            return null;
        });
    }

    public static void handleJa4Mismatch(SessionRecord record,
                                         String providedJa4,
                                         String currentIp,
                                         String currentUserAgent) {
        if (!RedisManager.isEnabled()) {
            return;
        }
        if (record != null) {
            record.setStatus(SessionStatus.CHALLENGE_REQUIRED);
            record.touch(Instant.now(), currentIp, currentUserAgent);
            persistSession(record);
        }
        Map<String, String> details = providedJa4 == null
                ? Collections.emptyMap()
                : Collections.singletonMap("receivedJa4", providedJa4);
        recordEvent(RiskEvent.create(
                RiskEventType.JA4_MISMATCH,
                record,
                currentIp,
                currentUserAgent,
                "JA4 fingerprint mismatch detected",
                details));
    }

    public static void recordAttributeChange(SessionRecord record,
                                             RiskEventType type,
                                             String message,
                                             Map<String, String> details,
                                             String currentIp,
                                             String currentUserAgent) {
        if (!RedisManager.isEnabled() || record == null) {
            return;
        }
        recordEvent(RiskEvent.create(type, record, currentIp, currentUserAgent, message, details));
    }

    public static void recordSessionTermination(SessionRecord record,
                                                String currentIp,
                                                String currentUserAgent,
                                                String reason) {
        if (!RedisManager.isEnabled()) {
            return;
        }
        if (record != null) {
            record.setStatus(SessionStatus.INVALIDATED);
            persistSession(record);
        }
        recordEvent(RiskEvent.create(
                RiskEventType.SESSION_TERMINATED,
                record,
                currentIp,
                currentUserAgent,
                reason,
                Collections.emptyMap()));
    }

    private static void recordEvent(RiskEvent event) {
        if (!RedisManager.isEnabled() || event == null) {
            return;
        }
        RedisManager.execute(jedis -> {
            jedis.lpush(RISK_EVENTS_KEY, GSON.toJson(event));
            jedis.ltrim(RISK_EVENTS_KEY, 0, EVENTS_HISTORY_LIMIT - 1);
            return null;
        });
    }
}
