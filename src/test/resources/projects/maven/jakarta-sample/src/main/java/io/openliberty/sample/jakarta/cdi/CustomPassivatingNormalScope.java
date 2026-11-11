package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.NormalScope;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@NormalScope(passivating = true)
@Retention(RUNTIME)
@Target({TYPE, METHOD, FIELD})
public @interface CustomPassivatingNormalScope {
}
