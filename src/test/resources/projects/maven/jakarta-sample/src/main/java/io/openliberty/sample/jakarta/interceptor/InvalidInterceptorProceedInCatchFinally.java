package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Invalid: proceed() is called inside the try block (protected), but also inside the
 * catch and finally blocks of the same try statement. Exceptions thrown from a catch or
 * finally block are not handled by that try statement, so those calls are unprotected.
 */
@Monitored
@Interceptor
public class InvalidInterceptorProceedInCatchFinally {

    @AroundInvoke
    public Object aroundInvoke(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed(); // inside try block, protected
        } catch (Exception ex) {
            return ctx.proceed(); // inside catch block, unprotected
        } finally {
            ctx.proceed(); // inside finally block, unprotected
        }
    }
}
