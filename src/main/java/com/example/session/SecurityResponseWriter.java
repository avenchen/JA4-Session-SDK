package com.example.session;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class SecurityResponseWriter {
    private static final Gson GSON = new Gson();

    private SecurityResponseWriter() {
    }

    public static void writeJson(HttpServletResponse response,
                                 int statusCode,
                                 String message,
                                 Map<String, Object> extra) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        if (extra != null) {
            payload.putAll(extra);
        }
        response.getWriter().write(GSON.toJson(payload));
    }
}
