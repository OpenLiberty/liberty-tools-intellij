package io.openliberty.sample.jakarta.cdi;

import jakarta.interceptor.Interceptor;
import jakarta.decorator.Decorator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;

// Invalid: @Interceptor with custom @NormalScope
@Interceptor
@CustomNormalScope
public class InterceptorDecoratorCustomScopes {
}

// Invalid: @Decorator with custom @NormalScope
@Decorator
@CustomNormalScope
class InvalidDecoratorWithCustomScope {
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

// Invalid: @Interceptor with mixed scopes (built-in and custom)
@Interceptor
@ApplicationScoped
@CustomNormalScope
class InvalidInterceptorWithMixedScopes {
}

// Invalid: @Decorator with mixed scopes (built-in and custom)
@Decorator
@ApplicationScoped
@CustomNormalScope
class InvalidDecoratorWithMixedScopes {
}

