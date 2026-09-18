package io.openliberty.sample.jakarta.interceptor;

import jakarta.interceptor.Interceptor;

/**
 * Valid: @Interceptor subclass of SeparateFileSuperclassWithAroundConstruct,
 * defined in a separate source file. Drives the project-wide scan to record
 * the ancestor's FQN, suppressing the diagnostic on the superclass file.
 */
@Monitored
@Interceptor
public class SeparateFileInterceptorSubclass extends SeparateFileSuperclassWithAroundConstruct {
}
