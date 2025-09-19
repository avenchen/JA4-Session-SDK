package com.example.session.sdk;

import com.example.session.FingerprintValidationFilter;
import com.example.session.LoginServlet;
import com.example.session.LogoutServlet;
import com.example.session.ProfileServlet;
import com.example.session.SessionConstants;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.Servlet;
import java.util.EnumSet;

public final class Ja4SessionSdk {
    private Ja4SessionSdk() {
    }

    public static void install(ServletContext context) {
        install(context, new Ja4SessionSdkOptions());
    }

    public static void install(ServletContext context, Ja4SessionSdkOptions options) {
        context.setAttribute(SessionConstants.CONTEXT_LOGIN_PATH, options.getLoginPath());
        context.setAttribute(SessionConstants.CONTEXT_PROFILE_PATH, options.getProfilePath());
        context.setAttribute(SessionConstants.CONTEXT_LOGOUT_PATH, options.getLogoutPath());

        registerServlet(context, "ja4LoginServlet", new LoginServlet(), options.getLoginPath());
        registerServlet(context, "ja4ProfileServlet", new ProfileServlet(), options.getProfilePath());
        registerServlet(context, "ja4LogoutServlet", new LogoutServlet(), options.getLogoutPath());

        FilterRegistration.Dynamic filter = context.addFilter("ja4FingerprintFilter", new FingerprintValidationFilter());
        filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, options.getProtectedPattern());
    }

    private static void registerServlet(ServletContext context, String name, Servlet servlet, String mapping) {
        ServletRegistration.Dynamic registration = context.addServlet(name, servlet);
        registration.addMapping(mapping);
        registration.setLoadOnStartup(1);
    }
}
