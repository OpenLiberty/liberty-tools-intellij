package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.InvocationContext;

/**
 * Valid: @AroundConstruct declared in a non-interceptor superclass whose
 * @Interceptor-annotated subclass is defined in a SEPARATE source file
 * (SeparateFileInterceptorSubclass.java).
 * ClassInheritorsSearch finds the @Interceptor subclass and suppresses
 * the diagnostic here.
 */
public class SeparateFileSuperclassWithAroundConstruct {

    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        ctx.proceed();
    }
}
