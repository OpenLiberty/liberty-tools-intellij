package io.openliberty.sample.jakarta.cdi.decorator;

import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/**
 * Invalid: @Delegate on field without @Inject
 */
@Decorator
@Dependent
class DelegateOnNonInjectedField implements PaymentService {
    
    @Delegate
    private PaymentService delegate;
    
    @Override
    public void processPayment(double amount) {
        delegate.processPayment(amount);
    }
}

/**
 * Invalid: @Delegate on method parameter without @Inject on method
 */
@Decorator
@Dependent
class DelegateOnNonInjectedMethodParam implements PaymentService {
    
    private PaymentService delegate;
    
    public void setDelegate(@Delegate PaymentService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void processPayment(double amount) {
        delegate.processPayment(amount);
    }
}

/**
 * Invalid: @Delegate on constructor parameter without @Inject on constructor
 */
@Decorator
@Dependent
class DelegateOnNonInjectedConstructorParam implements PaymentService {
    
    private PaymentService delegate;
    
    public DelegateOnNonInjectedConstructorParam(@Delegate PaymentService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void processPayment(double amount) {
        delegate.processPayment(amount);
    }
}

/**
 * Valid: @Delegate on field with @Inject
 */
@Decorator
@Dependent
class ValidDelegateOnInjectedField implements PaymentService {
    
    @Inject
    @Delegate
    private PaymentService delegate;
    
    @Override
    public void processPayment(double amount) {
        delegate.processPayment(amount);
    }
}

/**
 * Valid: @Delegate on constructor parameter with @Inject on constructor
 */
@Decorator
@Dependent
class ValidDelegateOnConstructorParam implements PaymentService {
    
    private PaymentService delegate;
    
    @Inject
    public ValidDelegateOnConstructorParam(@Delegate PaymentService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void processPayment(double amount) {
        delegate.processPayment(amount);
    }
}

/**
 * Valid: @Delegate on method parameter with @Inject on method
 */
@Decorator
@Dependent
class ValidDelegateOnMethodParam implements PaymentService {
    
    private PaymentService delegate;
    
    @Inject
    public void setDelegate(@Delegate PaymentService delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void processPayment(double amount) {
        delegate.processPayment(amount);
    }
}

/**
 * Invalid: @Delegate on parameter of an @Inject non-void method (non-initializer).
 * A non-void @Inject method is not an initializer method per CDI spec.
 */
@Decorator
@Dependent
class DelegateOnNonVoidInjectMethod implements PaymentService {

    @Inject
    public PaymentService buildDelegate(@Delegate PaymentService delegate) {
        return delegate;
    }

    @Override
    public void processPayment(double amount) {
    }
}

/**
 * Invalid: @Delegate on parameter of a static @Inject void method (non-initializer).
 * Static methods cannot be initializer methods per CDI spec.
 */
@Decorator
@Dependent
class DelegateOnStaticInjectMethod implements PaymentService {

    @Inject
    public static void setDelegate(@Delegate PaymentService delegate) {
    }

    @Override
    public void processPayment(double amount) {
    }
}
