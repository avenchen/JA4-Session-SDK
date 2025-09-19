package com.example.session;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;

public class LogoutServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            String sessionId = session.getId();
            Optional<SessionRecord> record = SessionSecurityRepository.getSession(sessionId);
            String clientIp = HttpRequestUtils.resolveClientIp(req);
            String userAgent = HttpRequestUtils.resolveUserAgent(req);
            record.ifPresent(r -> SessionSecurityRepository.recordSessionTermination(r, clientIp, userAgent, "User logout"));
            session.invalidate();
        }
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }
}

