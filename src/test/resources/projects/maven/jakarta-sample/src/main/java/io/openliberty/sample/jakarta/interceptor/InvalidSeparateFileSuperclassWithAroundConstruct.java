package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * Invalid: @AroundConstruct declared in a non-interceptor superclass whose
 * only subclass (InvalidSeparateFileTargetSubclass.java) is also non-interceptor.
 * No @Interceptor class extends this — diagnostic must fire.
 */
public class InvalidSeparateFileSuperclassWithAroundConstruct {

    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        ctx.proceed();
    }
}
