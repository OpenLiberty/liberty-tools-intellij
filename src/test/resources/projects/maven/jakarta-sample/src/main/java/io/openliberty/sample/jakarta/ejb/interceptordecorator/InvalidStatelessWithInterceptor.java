package io.openliberty.sample.jakarta.ejb.interceptordecorator;

import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptor;
import io.openliberty.sample.jakarta.interceptor.Monitored;

// Invalid: @Stateless with @Interceptor
@Stateless
@Interceptor
@Monitored
class InvalidStatelessWithInterceptor {
    public void businessMethod() {
    }
}
