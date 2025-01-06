package com.exadel.etoolbox.tracker.servlet;

import org.apache.http.HttpStatus;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import java.io.PrintWriter;
import java.io.Writer;

class MeasuringResponse extends SlingHttpServletResponseWrapper {

    private final PrintWriter printWriter;
    private int status;

    public MeasuringResponse(SlingHttpServletResponse response) {
        super(response);
        printWriter = new PrintWriter(Writer.nullWriter());
    }

    @Override
    public PrintWriter getWriter() {
        return printWriter;
    }

    @Override
    public void setContentType(String type) {
        // No operation. We prevent setting content type via the "original" response because we won't be able to
        // change it once it is set
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public int getStatus() {
        return status > 0 ? status : HttpStatus.SC_OK;
    }

    @Override
    public void setStatus(int value) {
        status = value;
    }

    @Override
    public void sendError(int sc) {
        setStatus(sc);
    }

    @Override
    public void sendError(int sc, String msg) {
        setStatus(sc);
    }
}
