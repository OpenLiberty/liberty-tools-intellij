package io.openliberty.sample.jakarta.cdi;

import jakarta.interceptor.Interceptor;
import io.openliberty.sample.jakarta.interceptor.Monitored;
import jakarta.enterprise.inject.Disposes;

@Monitored
@Interceptor
public class InterceptorWithDisposerMethod {

    // This method should trigger a diagnostic - interceptor with @Disposes
    public void disposerMethod(@Disposes String resource) {

    }

    // This method should not trigger a diagnostic - no disposer annotation
    public void normalMethod(String param) {

    }
}
