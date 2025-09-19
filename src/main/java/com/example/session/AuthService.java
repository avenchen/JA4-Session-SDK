package com.example.session;

import java.util.Map;
import java.util.Objects;

public final class AuthService {
    private static final Map<String, String> DEMO_USERS = Map.of(
            "admin", "admin123",
            "analyst", "risk4c$",
            "viewer", "viewer"
    );

    private AuthService() {
    }

    public static boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        String expected = DEMO_USERS.get(username);
        return expected != null && Objects.equals(expected, password);
    }
}
