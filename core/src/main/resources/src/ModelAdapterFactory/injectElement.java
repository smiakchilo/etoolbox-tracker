java.lang.RuntimeException injectElement(
        org.apache.sling.models.impl.model.InjectableElement element,
        Object adaptable,
        org.apache.sling.models.spi.DisposalCallbackRegistry registry,
        org.apache.sling.models.impl.ModelAdapterFactory.InjectCallback callback,
        java.util.Map preparedValues,
        org.osgi.framework.BundleContext modelContext) {

        String token = startTrack($args,"[inject]" + element.getName());
        java.lang.RuntimeException result = injectElementOriginal(element, adaptable, registry, callback, preparedValues, modelContext);
        endTrack($args, token);
        return result;
}