package com.exadel.etoolbox.tracker.service.impl;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
class ModelAdapterFactoryHook implements WeavingHook {

    private static final String METHOD_START_TRACK;
    private static final String METHOD_END_TRACK;
    private static final String METHOD_GET_MODEL_FROM_REQUEST;
    private static final String METHOD_GET_REQUEST;
    private static final String METHOD_CREATE_OBJECT;
    private static final String METHOD_INJECT_ELEMENT;
    static {
        METHOD_START_TRACK = getResourceContent("src/startTrack.java");
        METHOD_END_TRACK = getResourceContent("src/endTrack.java");
        METHOD_GET_MODEL_FROM_REQUEST = getResourceContent("src/ModelAdapterFactory/getModelFromWrappedRequest.java");
        METHOD_GET_REQUEST = getResourceContent("src/getAvailableRequest.java");
        METHOD_CREATE_OBJECT = getResourceContent("src/ModelAdapterFactory/createObject.java");
        METHOD_INJECT_ELEMENT = getResourceContent("src/ModelAdapterFactory/injectElement.java");
    }

    private static final ExprEditor POSTCONSTRUCT_INSTRUMENTATION = new PostConstructInstrumentation();

    @Override
    public void weave(WovenClass wovenClass) {
        String className = wovenClass.getClassName();
        if (!className.equals("org.apache.sling.models.impl.ModelAdapterFactory")
                || StringUtils.isAnyEmpty(METHOD_START_TRACK, METHOD_END_TRACK, METHOD_GET_REQUEST)) {
            return;
        }
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendClassPath(new LoaderClassPath(wovenClass.getBundleWiring().getClassLoader()));
        try {
            CtClass target = classPool.get(className);
            target.defrost();
            if (!methodExists(target, "getAvailableRequest")) {
                target.addMethod(CtMethod.make(METHOD_GET_REQUEST, target));
            }
            if (!methodExists(target, "startTrack")) {
                target.addMethod(CtMethod.make(METHOD_START_TRACK, target));
            }
            if (!methodExists(target, "endTrack")) {
                target.addMethod(CtMethod.make(METHOD_END_TRACK, target));
            }
            wrapMethod(target, "createObject", METHOD_CREATE_OBJECT, POSTCONSTRUCT_INSTRUMENTATION);
            wrapMethod(target, "injectElement", METHOD_INJECT_ELEMENT, null);
            wrapMethod(target, "getModelFromWrappedRequest", METHOD_GET_MODEL_FROM_REQUEST, null);
            wovenClass.setBytes(target.toBytecode());
        } catch (Exception e) {
            log.error("Could not modify {}", className, e);
        }
    }

    private static boolean methodExists(CtClass target, String name) {
        try {
            return target.getDeclaredMethod(name) != null;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private static void wrapMethod(
            CtClass target,
            String name,
            String wrapper,
            ExprEditor instrumentation) throws CannotCompileException, NotFoundException {

        String originalName = name + "Original";
        if (methodExists(target, originalName)) {
            return;
        }
        CtMethod method = target.getDeclaredMethod(name);
        method.setName(originalName);
        if (instrumentation != null) {
            method.instrument(instrumentation);
        }
        CtMethod newMethod = CtMethod.make(wrapper, target);
        target.addMethod(newMethod);
    }

    private static String getResourceContent(String name) {
        ClassLoader classLoader = ModelAdapterFactoryHook.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(name)) {
            return IOUtils.toString(Objects.requireNonNull(stream), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            log.error("Could not read resource {}", name, e);
        }
        return null;
    }

    private static class PostConstructInstrumentation extends ExprEditor {
        @Override
        public void edit(MethodCall method) throws CannotCompileException {
            if ("invokePostConstruct".equals(method.getMethodName())) {
                method.replace(" {  " +
                        "startTrack(new Object[]{adaptable},\"[postconstruct]Post-construct method|\" + modelClass.getType().getName()); " +
                        "$_ = $proceed($$); " +
                        "endTrack(new Object[]{adaptable},\"[postconstruct]Post-construct method|\" + modelClass.getType().getName()); " +
                        "} ");
            }
        }
    }
}
