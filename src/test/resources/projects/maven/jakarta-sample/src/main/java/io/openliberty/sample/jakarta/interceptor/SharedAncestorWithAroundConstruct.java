package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * Invalid: @AroundConstruct declared in a common ancestor of two non-interceptor
 * subclasses (NonInterceptorSubclassA, NonInterceptorSubclassB) in separate files.
 * Neither subclass is @Interceptor — diagnostic must fire.
 */
public class SharedAncestorWithAroundConstruct {

    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        ctx.proceed();
    }
}
