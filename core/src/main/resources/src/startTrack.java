private String startTrack(Object[] args, String token) {
    Object source = null;
    for (int i = 0; i < args.length; i++) {
        Object arg = args[i];
        if ((arg instanceof org.apache.sling.api.resource.Resource)
            || (arg instanceof org.apache.sling.api.SlingHttpServletRequest)) {
            source = arg;
            break;
        }
    }
    if (source == null || token == null) {
        return null;
    }
    org.apache.sling.api.SlingHttpServletRequest request = getAvailableRequest(source);
    if (request == null) {
        return null;
    }
    request.getRequestProgressTracker().startTimer(token);
    return token;
}
