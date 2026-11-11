package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * Valid: @AroundConstruct declared in a common ancestor of two subclasses in
 * separate files — one @Interceptor (InterceptorSubclassOfShared) and one
 * non-interceptor (NonInterceptorSubclassOfShared). The interceptor subclass
 * drives the scan — diagnostic must be suppressed.
 */
public class SharedAncestorInterceptorAndTarget {

    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        ctx.proceed();
    }
}
