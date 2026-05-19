package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Test class for @Valid annotation validation.
 * Tests both invalid usage (on non-cascadable types) and valid usage (on cascadable types).
 */
public class ValidAnnotationTest {

    // ========== INVALID USAGE - Should produce diagnostics ==========
    
    // Invalid: @Valid on primitive type
    @Valid
    private int primitiveInt;
    
    // Invalid: @Valid on boxed primitive type
    @Valid
    private Integer boxedInteger;
    
    // Invalid: @Valid on String
    @Valid
    private String stringField;
    
    // Invalid: @Valid on boxed Double
    @Valid
    private Double boxedDouble;
    
    // Invalid: @Valid on BigDecimal
    @Valid
    private BigDecimal bigDecimalField;
    
    // ========== VALID USAGE - Should NOT produce diagnostics ==========
    
    // Valid: @Valid on custom object (cascadable)
    @Valid
    private Address address;
    
    // Valid: @Valid on List (cascadable collection)
    @Valid
    private List<Address> addresses;
    
    // Valid: @Valid on array (cascadable)
    @Valid
    private Address[] addressArray;
    
    // Valid: @Valid on Map (cascadable)
    @Valid
    private Map<String, Address> addressMap;
    
    // ========== INVALID USAGE ON METHODS ==========
    
    // Invalid: @Valid on method returning primitive
    @Valid
    public boolean isPrimitiveReturn() {
        return true;
    }
    
    // ========== VALID USAGE ON METHODS ==========
    
    // Valid: @Valid on method returning custom object
    @Valid
    public Address getValidAddress() {
        return new Address();
    }
    
    // ========== INVALID USAGE ON PARAMETERS ==========
    
    // Invalid: @Valid on primitive parameter
    public void methodWithPrimitiveParam(@Valid int value) {
        // method body
    }
    
    // ========== VALID USAGE ON PARAMETERS ==========
    
    // Valid: @Valid on custom object parameter
    public void methodWithValidParam(@Valid Address addr) {
        // method body
    }
    
    // Inner class for testing
    static class Address {
        private String street;
        private String city;
    }
}