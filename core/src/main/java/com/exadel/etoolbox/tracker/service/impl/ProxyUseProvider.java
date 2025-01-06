package com.exadel.etoolbox.tracker.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestProgressTracker;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.render.RenderContext;
import org.apache.sling.scripting.sightly.use.ProviderOutcome;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.script.Bindings;
import java.util.Collection;
import java.util.NoSuchElementException;

@Slf4j
abstract class ProxyUseProvider implements UseProvider {

    @Override
    public ProviderOutcome provide(String identifier, RenderContext renderContext, Bindings bindings) {
        if (getUpstreamProvider() == null) {
            log.error("Upstream use provider is not available");
            return ProviderOutcome.failure();
        }
        Object requestObject = renderContext.getBindings().get(SlingBindings.REQUEST);
        RequestProgressTracker tracker = null;
        String logToken = "[" + getCategory() + "] " + identifier;
        if (requestObject instanceof SlingHttpServletRequest) {
            tracker = ((SlingHttpServletRequest) requestObject).getRequestProgressTracker();
            tracker.startTimer(logToken);
        }
        ProviderOutcome outcome = getUpstreamProvider().provide(identifier, renderContext, bindings);
        if (tracker != null) {
            tracker.logTimer(logToken, outcome.isFailure() ? "skip" : StringUtils.EMPTY);
        }
        return outcome;
    }

    abstract String getCategory();

    abstract UseProvider getUpstreamProvider();

    static UseProvider getUseProvider(BundleContext bundleContext, String name) {
        try {
            Collection<? extends ServiceReference<?>> references = bundleContext.getServiceReferences(
                    UseProvider.class,
                    "(component.name=" + name + ")");
            if (CollectionUtils.isEmpty(references)) {
                throw new NoSuchElementException("No use providers found for the given class name");
            }
            ServiceReference<?> reference = IteratorUtils.get(references.iterator(), 0);
            return (UseProvider) bundleContext.getService(reference);
        } catch (InvalidSyntaxException | NoSuchElementException e) {
            log.error("Could not get an instance of service {}", name, e);
        }
        return null;
    }
}
