package com.example.session.sdk;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class Ja4BootstrapListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Ja4SessionSdkOptions options = new Ja4SessionSdkOptions();
        Ja4SessionSdk.install(context, options);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // no-op
    }
}

