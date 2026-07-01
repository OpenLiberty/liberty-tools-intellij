package io.openliberty.sample.jakarta.ejb;

import jakarta.ejb.Stateless;
import jakarta.ejb.Stateful;
import jakarta.ejb.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.decorator.Decorator;

// Test case 1: Stateless with @Interceptor - should report error
@Stateless
@Interceptor
public class SessionBeanInterceptorDecorator {
}

// Test case 2: Stateless with @Decorator - should report error
@Stateless
@Decorator
class StatelessWithDecorator {
}

// Test case 3: Stateful with @Interceptor - should report error
@Stateful
@Interceptor
class StatefulWithInterceptor {
}

// Test case 4: Stateful with @Decorator - should report error
@Stateful
@Decorator
class StatefulWithDecorator {
}

// Test case 5: Singleton with @Interceptor - should report error
@Singleton
@Interceptor
class SingletonWithInterceptor {
}

// Test case 6: Singleton with @Decorator - should report error
@Singleton
@Decorator
class SingletonWithDecorator {
}

// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error
@Stateless
class ValidStatelessBeanNoConflict {
}
