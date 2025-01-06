private org.apache.sling.api.SlingHttpServletRequest getAvailableRequest(Object source) {
    if (source instanceof org.apache.sling.api.SlingHttpServletRequest) {
        return (org.apache.sling.api.SlingHttpServletRequest) source;
    }
    if (source instanceof org.apache.sling.api.resource.Resource) {
        Object referencedRequest = ((org.apache.sling.api.resource.Resource) source)
                .getResourceResolver()
                .getAttribute(org.apache.sling.api.SlingHttpServletRequest.class.getName());
        return (org.apache.sling.api.SlingHttpServletRequest) referencedRequest;
    }
    return null;
}
