package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.AroundTimeout;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Test resource: @AroundInvoke and @AroundTimeout with non-void/non-Object return types
 * must NOT trigger InvalidLifecycleCallbackInterceptorMethodSignature — they are not
 * lifecycle callbacks.
 */
@Monitored
@Interceptor
public class NonLifecycleInterceptorAnnotations {

    // @AroundInvoke with String return — NOT a lifecycle callback, no signature diagnostic
    @AroundInvoke
    public Object aroundInvoke(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }

    // @AroundTimeout with Object return — NOT a lifecycle callback, no signature diagnostic
    @AroundTimeout
    public Object aroundTimeout(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
