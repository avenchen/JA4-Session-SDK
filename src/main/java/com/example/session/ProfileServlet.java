package com.example.session;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProfileServlet extends HttpServlet {
    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No active session");
            return;
        }

        String username = (String) session.getAttribute(SessionConstants.SESSION_USER_ATTR);
        String fingerprint = (String) session.getAttribute(SessionConstants.SESSION_FINGERPRINT_ATTR);

        Map<String, Object> payload = new HashMap<>();
        payload.put("user", username);
        payload.put("sessionId", session.getId());
        payload.put("boundFingerprint", fingerprint);
        payload.put("issuedAt", Instant.ofEpochMilli(session.getCreationTime()).toString());
        payload.put("lastAccessedAt", Instant.ofEpochMilli(session.getLastAccessedTime()).toString());

        Optional<SessionRecord> record = SessionSecurityRepository.getSession(session.getId());
        record.ifPresent(r -> {
            payload.put("sessionStatus", r.getStatus().name());
            payload.put("firstSeenAt", r.getCreatedAt().toString());
            payload.put("lastSeenAt", r.getLastSeenAt().toString());
            payload.put("storedIp", r.getIpAddress());
            payload.put("storedUserAgent", r.getUserAgent());
            payload.put("clientSignals", r.getClientSignals());
        });

        resp.setContentType("application/json");
        try (PrintWriter writer = resp.getWriter()) {
            writer.write(GSON.toJson(payload));
        }
    }
}

