package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Date;
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
    
    // Invalid: @Valid on boxed primitive types
    @Valid
    private Integer boxedInteger;
    
    @Valid
    private Byte boxedByte;
    
    @Valid
    private Short boxedShort;
    
    @Valid
    private Long boxedLong;
    
    @Valid
    private Float boxedFloat;
    
    @Valid
    private Double boxedDouble;
    
    @Valid
    private Character boxedCharacter;
    
    @Valid
    private Boolean boxedBoolean;
    
    // Invalid: @Valid on String
    @Valid
    private String stringField;
    
    // Invalid: @Valid on BigDecimal and BigInteger
    @Valid
    private BigDecimal bigDecimalField;
    
    @Valid
    private BigInteger bigIntegerField;
    
    // Invalid: @Valid on Date/Time types
    @Valid
    private Date dateField;
    
    @Valid
    private LocalDate localDateField;
    
    // Invalid: @Valid on UUID, URI, URL (immutable value types)
    @Valid
    private java.util.UUID uuidField;
    
    @Valid
    private java.net.URI uriField;
    
    @Valid
    private java.net.URL urlField;
    
    // Invalid: @Valid on Enum types (non-cascadable)
    @Valid
    private Status enumField;
    
    // Invalid: @Valid on primitive arrays (non-cascadable)
    @Valid
    private int[] primitiveIntArray;
    
    @Valid
    private boolean[] primitiveBooleanArray;
    
    @Valid
    private double[] primitiveDoubleArray;
    
    // ========== VALID USAGE - Should NOT produce diagnostics ==========
    
    // Valid: @Valid on custom object (cascadable)
    @Valid
    private Address address;
    
    // Valid: @Valid on List (cascadable collection)
    @Valid
    private List<Address> addresses;
    
    // Valid: @Valid on object array (cascadable)
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
    
    // Invalid: @Valid on method returning boxed type
    @Valid
    public Long getBoxedLongReturn() {
        return 0L;
    }
    
    // Invalid: @Valid on method returning primitive array
    @Valid
    public int[] getPrimitiveArrayReturn() {
        return new int[0];
    }
    
    // ========== VALID USAGE ON METHODS ==========
    
    // Valid: @Valid on method returning custom object
    @Valid
    public Address getValidAddress() {
        return new Address();
    }
    
    // Valid: @Valid on method returning object array
    @Valid
    public Address[] getValidAddressArray() {
        return new Address[0];
    }
    
    // ========== INVALID USAGE ON PARAMETERS ==========
    
    // Invalid: @Valid on primitive parameter
    public void methodWithPrimitiveParam(@Valid int value) {
        // method body
    }
    
    // Invalid: @Valid on boxed type parameter
    public void methodWithBoxedParam(@Valid Integer value) {
        // method body
    }
    
    // Invalid: @Valid on primitive array parameter
    public void methodWithPrimitiveArrayParam(@Valid int[] values) {
        // method body
    }
    
    // ========== VALID USAGE ON PARAMETERS ==========
    
    // Valid: @Valid on custom object parameter
    public void methodWithValidParam(@Valid Address addr) {
        // method body
    }
    
    // Valid: @Valid on object array parameter
    public void methodWithValidArrayParam(@Valid Address[] addrs) {
        // method body
    }
    
    // Inner class for testing
    static class Address {
        private String street;
        private String city;
    }
    
    // Enum for testing
    enum Status {
        ACTIVE, INACTIVE, PENDING
    }
}