package io.openliberty.sample.jakarta.interceptor;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.interceptor.AroundConstruct;

/**
 * Target-class checks must also apply to nested classes at any depth:
 * - Level1.construct: @AroundConstruct is not allowed in a target class
 * - Level1.init: valid @PostConstruct void init() - no diagnostic expected
 * - Level2.cleanup: @PreDestroy must be void (second nesting level)
 */
public class InvalidTargetClassChecksInNestedClasses {

    public static class Level1 {

        @AroundConstruct
        public void construct() {
        }

        @PostConstruct
        public void init() {
        }

        public static class Level2 {

            @PreDestroy
            public String cleanup() {
                return "done";
            }
        }
    }
}
