package io.openliberty.sample.jakarta.interceptor;

/**
 * Non-interceptor subclass of InvalidSeparateFileSuperclassWithAroundConstruct,
 * defined in a separate file. Neither this class nor its superclass is @Interceptor,
 * so the diagnostic must still fire on the superclass file.
 */
public class InvalidSeparateFileTargetSubclass extends InvalidSeparateFileSuperclassWithAroundConstruct {
}
