package io.openliberty.sample.jakarta.annotations;

import random.test.pkg.on.Resource;

public class IncorrectResourceAnnotation {

    private Integer studentId;

    @Resource
    private boolean isSad;

    @random.test.pkg.on.Resource
    private boolean isHappy;

    private String emailAddress;

}