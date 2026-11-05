package com.example.annotation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Minimal stand-in for a container: finds and invokes methods annotated with
 * {@link PostConstruct} / {@link PreDestroy}.
 */
public final class LifecycleInvoker {

    private LifecycleInvoker() {
    }

    public static <T> T create(Class<T> type) throws ReflectiveOperationException {
        T instance = type.getDeclaredConstructor().newInstance();
        invoke(instance, PostConstruct.class);
        return instance;
    }

    public static void destroy(Object instance) throws ReflectiveOperationException {
        invoke(instance, PreDestroy.class);
    }

    private static void invoke(Object instance, Class<? extends Annotation> annotation)
            throws ReflectiveOperationException {
        for (Method m : instance.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotation)) {
                m.setAccessible(true);
                m.invoke(instance);
            }
        }
    }
}
