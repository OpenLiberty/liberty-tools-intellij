package io.openliberty.sample.jakarta.annotations;

import random.test.pkg.on.Generated;

@Generated(value = "demoServlet", date="")
public class IncorrectGeneratedAnnotation {

    @Generated(value = "demoServlet", date="not_ISO_compliant")
    private Integer studentId;

    @random.test.pkg.on.Generated(value = "demoServlet", date="2001-07-04T12:08:56.235-0700")
    private boolean isHappy;

}
