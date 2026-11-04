package io.openliberty.sample.jakarta.interceptor;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Monitored
@Interceptor
@Priority(-2100)
public class InvalidNegativePriorityInterceptor {

    @AroundInvoke
    public Object log(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed();
        } catch (Exception e) {
            throw e;
        }
    }

    // Inner class with negative priority
    @Monitored
    @Interceptor
    @Priority(-500)
    public static class InnerInvalidPriorityInterceptor {
        
        @AroundInvoke
        public Object log(InvocationContext ctx) throws Exception {
            try {
                return ctx.proceed();
            } catch (Exception e) {
                throw e;
            }
        }
    }
}
