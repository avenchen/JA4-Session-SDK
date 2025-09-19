package com.example.session;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginServlet.class);
    private static final Gson GSON = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LoginRequest loginRequest;
        try {
            loginRequest = GSON.fromJson(request.getReader(), LoginRequest.class);
        } catch (JsonSyntaxException ex) {
            LOGGER.warn("Failed to parse login payload", ex);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON body");
            return;
        }

        if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing credentials");
            return;
        }

        boolean authenticated = AuthService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        if (!authenticated) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid username or password");
            return;
        }

        String ja4Fingerprint = Ja4FingerprintExtractor.extract(request);
        HttpSession session = request.getSession(true);
        session.setAttribute(SessionConstants.SESSION_USER_ATTR, loginRequest.getUsername());
        session.setAttribute(SessionConstants.SESSION_FINGERPRINT_ATTR, ja4Fingerprint);
        session.setMaxInactiveInterval(900); // 15 minutes

        String clientIp = HttpRequestUtils.resolveClientIp(request);
        String userAgent = HttpRequestUtils.resolveUserAgent(request);
        Map<String, String> clientSignals = Optional.ofNullable(loginRequest.getClientSignals()).orElse(Collections.emptyMap());
        String clientFingerprint = Optional.ofNullable(loginRequest.getClientFingerprint()).orElse(ja4Fingerprint);

        SessionRecord record = SessionRecord.create(
                session.getId(),
                loginRequest.getUsername(),
                ja4Fingerprint,
                clientFingerprint,
                userAgent,
                clientIp,
                clientSignals);
        SessionSecurityRepository.persistSession(record);

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "success");
        payload.put("user", loginRequest.getUsername());
        payload.put("boundFingerprint", ja4Fingerprint);
        payload.put("clientFingerprint", clientFingerprint);
        payload.put("clientSignals", clientSignals);
        payload.put("ipAddress", clientIp);
        payload.put("userAgent", userAgent);
        payload.put("sessionStatus", record.getStatus().name());
        payload.put("lastSeenAt", Instant.now().toString());

        response.setContentType("application/json");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(GSON.toJson(payload));
        }
        LOGGER.info("User {} successfully logged in with JA4 {}", loginRequest.getUsername(), ja4Fingerprint);
    }
}
