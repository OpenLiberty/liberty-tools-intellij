package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Invalid: Interceptor declared with @Interceptor but missing interceptor binding annotation.
 * This should trigger a diagnostic error.
 */
@Interceptor
public class InvalidInterceptorMissingBinding {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
