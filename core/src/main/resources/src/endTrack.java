private void endTrack(Object[] args, String token) {
    if (args == null || token == null) {
        return;
    }
    Object source = null;
    for (int i = 0; i < args.length; i++) {
        Object arg = args[i];
        if ((arg instanceof org.apache.sling.api.resource.Resource)
            || (arg instanceof org.apache.sling.api.SlingHttpServletRequest)) {
            source = arg;
            break;
        }
    }
    org.apache.sling.api.SlingHttpServletRequest request = getAvailableRequest(source);
    if (request == null) {
        return;
    }
    request.getRequestProgressTracker().logTimer(token);
}