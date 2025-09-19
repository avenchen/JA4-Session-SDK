package com.example.session;

import java.util.Map;

public class LoginRequest {
    private String username;
    private String password;
    private String clientFingerprint;
    private Map<String, String> clientSignals;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getClientFingerprint() {
        return clientFingerprint;
    }

    public Map<String, String> getClientSignals() {
        return clientSignals;
    }
}
