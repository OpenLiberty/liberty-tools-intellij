package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * The outer class is an @Interceptor, so @AroundConstruct is valid in it.
 * The static nested class is not annotated with @Interceptor, so it is a
 * target class and @AroundConstruct is not allowed in it.
 */
@Monitored
@Interceptor
public class InterceptorWithNestedTargetClass {

    @AroundConstruct
    public void construct(InvocationContext ctx) throws Exception {
        ctx.proceed();
    }

    public static class NestedTarget {

        @AroundConstruct
        public void nestedConstruct() {
        }
    }
}
