package io.openliberty.sample.jakarta.cdi;

import jakarta.interceptor.Interceptor;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;

@Interceptor
public class InterceptorWithObserverMethod {
    
    // This method should trigger a diagnostic - interceptor with @Observes
    public void observerMethod(@Observes String event) {
        System.out.println("Observer method called with: " + event);
    }
    
    // This method should also trigger a diagnostic - interceptor with @ObservesAsync
    public void asyncObserverMethod(@ObservesAsync String event) {
        System.out.println("Async observer method called with: " + event);
    }
    
    // This method should not trigger a diagnostic - no observer annotations
    public void normalMethod(String param) {
        System.out.println("Normal method called with: " + param);
    }
}

// Made with Bob
