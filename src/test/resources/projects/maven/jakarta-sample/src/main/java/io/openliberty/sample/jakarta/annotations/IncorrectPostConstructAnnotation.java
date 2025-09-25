package io.openliberty.sample.jakarta.annotations;

import jakarta.annotation.Resource;
import random.test.pkg.on.PostConstruct;

@Resource(type = Object.class, name = "aa")
public class IncorrectPostConstructAnnotation {

    @PostConstruct
    public void getHappinessRandom(String type) {

    }

    @random.test.pkg.on.PostConstruct
    public void getRandom(String type) {

    }

}