package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.NormalScope;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom scope annotation meta-annotated with @NormalScope.
 * This is used to test detection of custom normal scopes on interceptors and decorators.
 */
@NormalScope
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD})
public @interface CustomNormalScope {
}

