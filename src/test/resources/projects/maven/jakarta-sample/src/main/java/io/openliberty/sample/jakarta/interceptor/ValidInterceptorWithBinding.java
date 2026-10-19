package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Valid: Interceptor declared with @Interceptor and has @Monitored interceptor binding.
 * This should NOT trigger any diagnostic.
 */
@Monitored
@Interceptor
public class ValidInterceptorWithBinding {

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        return ctx.proceed();
    }
}
