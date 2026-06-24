package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

@Entity
public class EntityValidIdTypes {

    // Valid: primitive int
    @Id
    private int intId;

    // Valid: primitive long
    @Id
    private long longId;

    // Valid: primitive byte
    @Id
    private byte byteId;

    // Valid: primitive short
    @Id
    private short shortId;

    // Valid: primitive char
    @Id
    private char charId;

    // Valid: wrapper Integer
    @Id
    private Integer integerId;

    // Valid: wrapper Long
    @Id
    private Long longWrapperId;

    // Valid: wrapper Byte
    @Id
    private Byte byteWrapperId;

    // Valid: wrapper Short
    @Id
    private Short shortWrapperId;

    // Valid: wrapper Character
    @Id
    private Character charWrapperId;

    // Valid: String
    @Id
    private String stringId;

    // Valid: java.util.Date with @Temporal
    @Id
    @Temporal(TemporalType.DATE)
    private Date utilDateId;

    // Valid: java.sql.Date (doesn't require @Temporal)
    @Id
    private java.sql.Date sqlDateId;

    // Valid: BigDecimal
    @Id
    private BigDecimal bigDecimalId;

    // Valid: BigInteger
    @Id
    private BigInteger bigIntegerId;

    public EntityValidIdTypes() {
    }
}

// Made with Bob
