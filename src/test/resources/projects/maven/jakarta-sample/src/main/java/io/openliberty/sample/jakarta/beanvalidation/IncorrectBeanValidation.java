package io.openliberty.sample.jakarta.beanvalidation;

import random.test.pkg.ts.*;

public class IncorrectBeanValidation {

    @AssertTrue
    private int isHappy;

    @AssertFalse
    private Double isSad;

    @Email
    private Integer emailAddress;

    @FutureOrPresent
    private boolean graduationDate;

    @Future
    private double fergiesYear;

    @Negative
    private boolean subZero;

    @Positive
    private String[] area;

    @AssertTrue
    private static boolean typeValid;

    @random.test.pkg.ts.AssertTrue
    private int isHappyFq;

    @random.test.pkg.ts.AssertFalse
    private Double isSadFq;

    @random.test.pkg.ts.Email
    private Integer emailAddressFq;

    @random.test.pkg.ts.FutureOrPresent
    private boolean graduationDateFq;

    @random.test.pkg.ts.Future
    private double fergiesYearFq;

    @random.test.pkg.ts.Negative
    private boolean subZeroFq;

    @random.test.pkg.ts.Positive
    private String[] areaFq;

    @random.test.pkg.ts.AssertTrue
    private static boolean typeValidFq;

    @AssertTrue
    public static boolean anotherTruth() {  // static
        return true;
    }

    @AssertTrue
    public String notBoolean() {            // invalid type
        return "aha!";
    }

    @random.test.pkg.ts.AssertTrue
    public static boolean anotherTruthFq() {  // static
        return true;
    }

    @random.test.pkg.ts.AssertTrue
    public String notBooleanFq() {            // invalid type
        return "aha!";
    }
}