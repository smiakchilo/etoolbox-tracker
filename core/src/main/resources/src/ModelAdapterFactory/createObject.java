private org.apache.sling.models.impl.Result createObject(
        Object adaptable,
        org.apache.sling.models.impl.model.ModelClass modelClass)
        throws java.lang.InstantiationException, java.lang.reflect.InvocationTargetException, java.lang.IllegalAccessException {
    String token = startTrack($args,"[model]" + modelClass.getType().getName());
    org.apache.sling.models.impl.Result result = createObjectOriginal(adaptable, modelClass);
    endTrack($args, token);
    return result;
}