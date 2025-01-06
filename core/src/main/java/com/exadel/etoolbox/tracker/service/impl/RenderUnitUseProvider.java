package com.exadel.etoolbox.tracker.service.impl;

import com.exadel.etoolbox.tracker.service.Registrable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sling.scripting.sightly.use.UseProvider;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;

@RequiredArgsConstructor
@Slf4j
class RenderUnitUseProvider extends ProxyUseProvider implements Registrable {

    private static final Dictionary<String, Object> PROPERTIES;
    static {
        PROPERTIES = new Hashtable<>();
        PROPERTIES.put("service.ranking", 101);
    }

    private final BundleContext bundleContext;

    @Override
    public Dictionary<String, Object> getProperties() {
        return PROPERTIES;
    }

    @Override
    String getCategory() {
        return "embed";
    }

    @Override
    UseProvider getUpstreamProvider() {
        return getUseProvider(
                bundleContext,
                "org.apache.sling.scripting.sightly.impl.engine.extension.use.RenderUnitProvider");

    }
}
