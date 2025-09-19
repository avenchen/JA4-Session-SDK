package com.example.session.sdk;

public class Ja4SessionSdkOptions {
    private String loginPath = "/api/login";
    private String profilePath = "/api/profile";
    private String logoutPath = "/api/logout";
    private String protectedPattern = "/api/*";

    public String getLoginPath() {
        return loginPath;
    }

    public Ja4SessionSdkOptions setLoginPath(String loginPath) {
        this.loginPath = loginPath;
        return this;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public Ja4SessionSdkOptions setProfilePath(String profilePath) {
        this.profilePath = profilePath;
        return this;
    }

    public String getLogoutPath() {
        return logoutPath;
    }

    public Ja4SessionSdkOptions setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
        return this;
    }

    public String getProtectedPattern() {
        return protectedPattern;
    }

    public Ja4SessionSdkOptions setProtectedPattern(String protectedPattern) {
        this.protectedPattern = protectedPattern;
        return this;
    }
}
