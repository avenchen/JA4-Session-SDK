package com.example.session;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FingerprintValidationFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FingerprintValidationFilter.class);

    @Override
    public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String loginPath = Optional.ofNullable((String) request.getServletContext().getAttribute(SessionConstants.CONTEXT_LOGIN_PATH))
                .orElse("/api/login");

        if (matchesRequestPath(request, loginPath)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            LOGGER.debug("Rejecting request without session for {}", request.getRequestURI());
            SecurityResponseWriter.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "No active session", Map.of("action", "LOGIN"));
            return;
        }

        String storedFingerprint = (String) session.getAttribute(SessionConstants.SESSION_FINGERPRINT_ATTR);
        if (storedFingerprint == null) {
            LOGGER.warn("Session {} missing fingerprint binding", session.getId());
            session.invalidate();
            SecurityResponseWriter.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Fingerprint not bound to session", Map.of("action", "LOGIN"));
            return;
        }

        String currentFingerprint = Ja4FingerprintExtractor.extract(request);
        String currentIp = HttpRequestUtils.resolveClientIp(request);
        String currentUserAgent = HttpRequestUtils.resolveUserAgent(request);

        Optional<SessionRecord> recordOptional = SessionSecurityRepository.getSession(session.getId());
        SessionRecord record = recordOptional.orElse(null);

        if (!storedFingerprint.equals(currentFingerprint)) {
            LOGGER.warn("Fingerprint mismatch for session {}. Expected {}, received {}.",
                    session.getId(), storedFingerprint, currentFingerprint);
            SessionSecurityRepository.handleJa4Mismatch(record, currentFingerprint, currentIp, currentUserAgent);
            session.invalidate();
            response.setHeader("X-Session-Challenge", "VERIFY_JA4");
            Map<String, Object> detail = new HashMap<>();
            detail.put("error", "Fingerprint mismatch");
            detail.put("action", "VERIFY_JA4");
            SecurityResponseWriter.writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "JA4 verification required", detail);
            return;
        }

        if (record != null) {
            boolean attributeChanged = false;
            if (record.isDifferentIp(currentIp)) {
                attributeChanged = true;
                SessionSecurityRepository.recordAttributeChange(record,
                        RiskEventType.CLIENT_ATTRIBUTE_CHANGE,
                        "Client IP changed during session",
                        Map.of("previousIp", Optional.ofNullable(record.getIpAddress()).orElse("unknown"),
                                "currentIp", currentIp),
                        currentIp,
                        currentUserAgent);
            }
            if (record.isDifferentUserAgent(currentUserAgent)) {
                attributeChanged = true;
                SessionSecurityRepository.recordAttributeChange(record,
                        RiskEventType.CLIENT_ATTRIBUTE_CHANGE,
                        "User-Agent changed during session",
                        Map.of("previousUserAgent", Optional.ofNullable(record.getUserAgent()).orElse("unknown"),
                                "currentUserAgent", currentUserAgent),
                        currentIp,
                        currentUserAgent);
            }

            if (attributeChanged) {
                LOGGER.warn("Session {} attributes changed (IP/UA)", session.getId());
            }
            SessionSecurityRepository.refreshSession(record, currentIp, currentUserAgent);
        }

        chain.doFilter(request, response);
    }

    private boolean matchesRequestPath(HttpServletRequest request, String configuredPath) {
        String servletPath = request.getServletPath();
        if (configuredPath.endsWith("/*")) {
            String prefix = configuredPath.substring(0, configuredPath.length() - 1);
            return servletPath.startsWith(prefix);
        }
        return servletPath.equals(configuredPath);
    }
}
