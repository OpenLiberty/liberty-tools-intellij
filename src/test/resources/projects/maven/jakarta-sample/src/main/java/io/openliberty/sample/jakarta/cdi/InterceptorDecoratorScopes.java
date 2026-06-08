package io.openliberty.sample.jakarta.cdi;

import jakarta.interceptor.Interceptor;
import jakarta.decorator.Decorator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.enterprise.context.Dependent;

// Invalid: @Interceptor with @ApplicationScoped
@Interceptor
@ApplicationScoped
public class InterceptorDecoratorScopes {
}

// Invalid: @Decorator with @RequestScoped
@Decorator
@RequestScoped
class InvalidDecoratorWithRequestScoped {
}

// Valid: @Interceptor with @Dependent
@Interceptor
@Dependent
class ValidInterceptorWithDependent {
}

// Valid: @Decorator with @Dependent
@Decorator
@Dependent
class ValidDecoratorWithDependent {
}

// Invalid: @Interceptor with @Dependent and @SessionScoped (has invalid scope)
@Interceptor
@Dependent
@SessionScoped
class InvalidInterceptorWithMultipleScopes {
}

// Valid: @Decorator with no scope annotation (defaults to @Dependent)
@Decorator
class ValidDecoratorWithNoScope {
}

// Valid: @Interceptor with no scope annotation (defaults to @Dependent)
@Interceptor
class ValidInterceptorWithNoScope {
}
