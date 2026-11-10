package io.openliberty.sample.jakarta.ejb.interceptordecorator;

import jakarta.interceptor.Interceptor;
import io.openliberty.sample.jakarta.interceptor.Monitored;

// Valid: @Interceptor without session bean annotation
@Monitored
@Interceptor
class ValidInterceptor {
    public void intercept() {
    }
}
