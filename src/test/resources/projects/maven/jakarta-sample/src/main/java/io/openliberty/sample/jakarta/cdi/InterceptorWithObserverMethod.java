package io.openliberty.sample.jakarta.cdi;

import io.openliberty.sample.jakarta.interceptor.Monitored;
import jakarta.interceptor.Interceptor;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

@Monitored
@Interceptor
public class InterceptorWithObserverMethod {
    
    // This method should trigger a diagnostic - interceptor with @Observes
    public void observerMethod(@Observes String event) {

    }
    
    // This method should also trigger a diagnostic - interceptor with @ObservesAsync
    public void asyncObserverMethod(@ObservesAsync String event) {

    }
    
    // This method should not trigger a diagnostic - no observer annotations
    public void normalMethod(String param) {

    }
}
