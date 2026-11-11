package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.Interceptor;

/**
 * @Interceptor subclass of SharedAncestorInterceptorAndTarget.
 * Its presence is discovered by ClassInheritorsSearch, suppressing
 * the diagnostic on the ancestor file.
 */
@Monitored
@Interceptor
public class InterceptorSubclassOfShared extends SharedAncestorInterceptorAndTarget {
}
