package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.Interceptor;

/**
 * @Interceptor subclass of SharedAncestorInterceptorAndTarget.
 * Drives the project-wide scan to record the ancestor's FQN,
 * suppressing the diagnostic on the ancestor file.
 */
@Monitored
@Interceptor
public class InterceptorSubclassOfShared extends SharedAncestorInterceptorAndTarget {
}
