package com.exadel.etoolbox.tracker.servlet;

import com.exadel.etoolbox.tracker.service.HookManager;
import com.exadel.etoolbox.tracker.util.TrackWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;

@Component(
        service = Servlet.class,
        property = {
                ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + TrackServlet.PATH,
                ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=json"})
@Slf4j
public class TrackServlet extends SlingSafeMethodsServlet {

    public static final String PATH = "/apps/etoolbox-tracker/track";

    @Reference
    private HookManager serviceManager;

    @Override
    protected void doGet(
            @NotNull SlingHttpServletRequest request,
            @NotNull SlingHttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    private void processRequest(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        SlingHttpServletRequest measuringRequest = new MeasuringRequest(request);
        SlingHttpServletResponse measuringResponse = new MeasuringResponse(response);
        String measuringPath = measuringRequest.getPathInfo();

        RequestDispatcher requestDispatcher = request.getRequestDispatcher(measuringPath);
        int status;
        try {
            requestDispatcher.forward(measuringRequest, measuringResponse);
            status = measuringResponse.getStatus();
        } catch (ServletException | IOException e) {
            log.error("Could not execute measuring request", e);
            response.sendError(HttpStatus.SC_SERVICE_UNAVAILABLE);
            return;
        } catch (ResourceNotFoundException e) {
            log.warn("Path {} not found", request.getRequestURI());
            status = HttpStatus.SC_NOT_FOUND;
        }

        response.setContentType("application/json");
        try {
            TrackWriter.builder()
                    .request(request)
                    .status(status)
                    .hooksStatus(serviceManager.getStatus())
                    .build()
                    .writeTo(response);
        } catch (IOException e) {
            log.error("Could not output track info", e);
            response.sendError(HttpStatus.SC_SERVICE_UNAVAILABLE);
        }
    }
}