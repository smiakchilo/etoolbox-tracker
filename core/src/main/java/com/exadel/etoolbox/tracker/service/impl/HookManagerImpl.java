package com.exadel.etoolbox.tracker.service.impl;

import com.exadel.etoolbox.tracker.service.HookManager;
import com.exadel.etoolbox.tracker.service.Registrable;
import com.exadel.etoolbox.tracker.service.RegistrationStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component(service = HookManager.class)
@Slf4j
public class HookManagerImpl implements HookManager {

    private static final List<String> MANAGED_BUNDLES = List.of(
            "org.apache.sling.models.impl"
    );

    private static final int BUNDLE_REFRESH_TIMEOUT_SEC = 60;

    private BundleContext bundleContext;
    private List<ServiceRegistration<?>> registrations;

    @Activate
    private void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Deactivate
    private void deactivate() {
        unregisterServices();
    }

    @Override
    public RegistrationStatus getStatus() {
        return CollectionUtils.isNotEmpty(registrations) ? RegistrationStatus.ON : RegistrationStatus.OFF;
    }

    @Override
    public void registerServices() {
        if (CollectionUtils.isNotEmpty(registrations)) {
            return;
        }
        if (registrations == null) {
            registrations = new ArrayList<>();
        }
        registrations.add(registerService(new ModelAdapterFactoryHook(), WeavingHook.class));
        registrations.add(registerService(new JavaPojoProvider(bundleContext), UseProvider.class));
        registrations.add(registerService(new RenderUnitUseProvider(bundleContext), UseProvider.class));
        refreshBundles();
    }

    private ServiceRegistration<?> registerService(Object service, Class<?> ... types) {
        Dictionary<String, Object> properties = new Hashtable<>();
        if (service instanceof Registrable) {
            Registrable registrable = (Registrable) service;
            properties = registrable.getProperties();
        }
        String[] typeNames = Arrays.stream(types).map(Class::getName).toArray(String[]::new);
        return bundleContext.registerService(typeNames, service, properties);
    }

    @Override
    public void unregisterServices() {
        if (CollectionUtils.isEmpty(registrations)) {
            return;
        }
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }
        registrations.clear();
        refreshBundles();
    }

    private Bundle getBundle(String name) {
        return Arrays.stream(bundleContext.getBundles())
                .filter(b -> name.equals(b.getSymbolicName()))
                .findFirst()
                .orElse(null);
    }

    private void refreshBundles() {
        Bundle systemBundle = bundleContext.getBundle(0);
        if (systemBundle == null) {
            return;
        }
        FrameworkWiring wiring = systemBundle.adapt(FrameworkWiring.class);
        List<Bundle> bundles = MANAGED_BUNDLES
                .stream()
                .map(this::getBundle)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        CompletableFuture<Void> future = refreshBundlesAsync(bundles, wiring);
        try {
            future.get(BUNDLE_REFRESH_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Could not complete bundles refreshing", e);
        } catch (TimeoutException e) {
            log.warn("Bundle refreshing took over {} s", BUNDLE_REFRESH_TIMEOUT_SEC, e);
        }
    }

    private CompletableFuture<Void> refreshBundlesAsync(List<Bundle> bundles, FrameworkWiring wiring) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        wiring.refreshBundles(
                bundles,
                frameworkEvent -> {
                    if (frameworkEvent.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                        future.complete(null);
                    }
                });
        return future;
    }
}
