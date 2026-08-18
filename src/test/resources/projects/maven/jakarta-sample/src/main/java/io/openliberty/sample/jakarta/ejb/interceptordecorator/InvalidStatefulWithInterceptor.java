package io.openliberty.sample.jakarta.ejb.interceptordecorator;

import jakarta.ejb.Stateful;
import jakarta.interceptor.Interceptor;
import io.openliberty.sample.jakarta.interceptor.Monitored;

// Invalid: @Stateful with @Interceptor
@Stateful
@Interceptor
@Monitored
class InvalidStatefulWithInterceptor {
    public void businessMethod() {
    }
}
