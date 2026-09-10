package io.openliberty.sample.jakarta.annotations;

import random.test.pkg.on.Generated;

@Generated()
public class IncorrectGeneratedAnnotation {

    @Generated()
    private Integer studentId;

    @random.test.pkg.on.Generated()
    private boolean isHappy;

}
