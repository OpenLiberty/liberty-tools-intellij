package io.openliberty.sample.jakarta.interceptor;

/**
 * Non-interceptor sibling subclass of SharedAncestorInterceptorAndTarget.
 * Even though this class is not @Interceptor, its sibling InterceptorSubclassOfShared
 * is, so the ancestor's diagnostic is still suppressed.
 */
public class NonInterceptorSubclassOfShared extends SharedAncestorInterceptorAndTarget {
}
