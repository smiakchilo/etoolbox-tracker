package com.exadel.etoolbox.tracker.servlet;

import com.exadel.etoolbox.tracker.service.HookManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
        service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=POST",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/apps/etoolbox-tracker/hooks/status",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/apps/etoolbox-tracker/hooks/on",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/apps/etoolbox-tracker/hooks/off"
        })
public class HooksServlet extends SlingAllMethodsServlet {
    private static final String STATUS = "{\"status\":\"%s\"}%n";

    @Reference
    private HookManager proxyManager;

    @Override
    protected void doGet(
            @NotNull SlingHttpServletRequest request,
            @NotNull SlingHttpServletResponse response) throws IOException {
        if (request.getRequestURI().endsWith("/status")) {
            response.setContentType("application/json");
            response.getWriter().printf(STATUS, proxyManager.getStatus().toString());
            response.getWriter().flush();
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doPost(
            @NotNull SlingHttpServletRequest request,
            @NotNull SlingHttpServletResponse response) throws IOException {
        if (request.getRequestURI().endsWith("/on")) {
            proxyManager.registerServices();
            response.getWriter().printf(STATUS, proxyManager.getStatus().toString());
            response.getWriter().flush();
        } else if (request.getRequestURI().endsWith("/off")) {
            proxyManager.unregisterServices();
            response.getWriter().printf(STATUS, proxyManager.getStatus().toString());
            response.getWriter().flush();
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

}
