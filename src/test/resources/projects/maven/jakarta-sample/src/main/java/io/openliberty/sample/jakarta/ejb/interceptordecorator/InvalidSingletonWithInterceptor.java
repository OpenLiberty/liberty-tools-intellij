package io.openliberty.sample.jakarta.ejb.interceptordecorator;

import jakarta.ejb.Singleton;
import jakarta.interceptor.Interceptor;
import io.openliberty.sample.jakarta.interceptor.Monitored;

// Invalid: @Singleton with @Interceptor
@Singleton
@Interceptor
@Monitored
class InvalidSingletonWithInterceptor {
    public void businessMethod() {
    }
}
