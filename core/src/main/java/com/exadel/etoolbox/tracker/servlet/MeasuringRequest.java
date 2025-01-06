package com.exadel.etoolbox.tracker.servlet;

import com.day.cq.commons.PathInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ResourceResolverWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.util.regex.Pattern;

class MeasuringRequest extends SlingHttpServletRequestWrapper {

    private static final Pattern EXTENSION_PATTERN = Pattern.compile("\\.\\w+$");

    private static final String PROTOCOL_DELIMITER = "//";
    private static final String SCHEMA_DELIMITER = "://";
    private static final String SLASH = "/";

    private final ResourceResolver resolver;
    private final RequestPathInfo requestPathInfo;
    private final String pathInfo;

    MeasuringRequest(SlingHttpServletRequest request) {
        super(request);
        resolver = new LocalResourceResolver(request);
        pathInfo = extractPathInfo(request.getRequestPathInfo().getSuffix());
        requestPathInfo = new PathInfo(pathInfo);
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public @NotNull RequestPathInfo getRequestPathInfo() {
        return requestPathInfo;
    }

    @Override
    public @NotNull ResourceResolver getResourceResolver() {
        return resolver;
    }

    private static String extractPathInfo(String source) {
        String result = StringUtils.defaultString(source);
        if (result.contains(SCHEMA_DELIMITER)) {
            result = result.substring(result.indexOf(SCHEMA_DELIMITER) + SCHEMA_DELIMITER.length());
            result = result.contains(SLASH) ? result.substring(result.indexOf(SLASH)) : result;
        } else if (result.startsWith(PROTOCOL_DELIMITER)) {
            result = result.substring(PROTOCOL_DELIMITER.length());
            result = result.contains(SLASH) ? result.substring(result.indexOf(SLASH)) : result;
        }
        if (!result.isEmpty() && !SLASH.equals(result) && !EXTENSION_PATTERN.matcher(source).find()) {
            result += ".html";
        }
        return result;
    }

    private static class LocalResourceResolver extends ResourceResolverWrapper {

        private final SoftReference<SlingHttpServletRequest> request;

        LocalResourceResolver(SlingHttpServletRequest request) {
            super(request.getResourceResolver());
            this.request = new SoftReference<>(request);
        }

        @Override
        public Object getAttribute(@NotNull String name) {
            if (SlingHttpServletRequest.class.getName().equals(name)) {
                return request != null ? request.get() : null;
            }
            return super.getAttribute(name);
        }
    }
}
