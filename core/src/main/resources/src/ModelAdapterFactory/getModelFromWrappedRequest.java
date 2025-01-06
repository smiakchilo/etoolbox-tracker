public Object getModelFromWrappedRequest(
        org.apache.sling.api.SlingHttpServletRequest request,
        org.apache.sling.api.resource.Resource resource,
        Class targetClass) {

        String token = startTrack($args, "[wrapper]Synthetic request|Wraps resource at " + resource.getPath());
        Object result = getModelFromWrappedRequestOriginal(request, resource, targetClass);
        endTrack($args, token);
        return result;
}