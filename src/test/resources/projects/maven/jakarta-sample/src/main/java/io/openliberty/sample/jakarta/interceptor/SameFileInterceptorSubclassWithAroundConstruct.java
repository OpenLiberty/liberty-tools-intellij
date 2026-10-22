package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Valid: @AroundConstruct declared in a non-interceptor superclass whose
 * @Interceptor-annotated subclass lives in the SAME compilation unit.
 *
 * hasInterceptorSubclass() includes same-file subtypes, so the @Interceptor
 * subclass below IS discovered and both diagnostics are suppressed.
 */
class SameFileSuperclassWithAroundConstruct {

    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        ctx.proceed();
    }
}

/**
 * The @Interceptor subclass is in the same file as the superclass above.
 * hasInterceptorSubclass() discovers it, so the superclass is treated as
 * an interceptor superclass and no diagnostic fires.
 */
@Monitored
@Interceptor
public class SameFileInterceptorSubclassWithAroundConstruct extends SameFileSuperclassWithAroundConstruct {
    // Subclass is a proper @Interceptor — but lives in the same compilation unit
    // as the superclass, so hasInterceptorSubclass() returns false for the superclass.
}
