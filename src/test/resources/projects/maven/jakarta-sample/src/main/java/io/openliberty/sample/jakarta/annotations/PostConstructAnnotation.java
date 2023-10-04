package io.openliberty.sample.jakarta.annotations;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Resource(type = Object.class, name = "aa")
public class PostConstructAnnotation {

    private Integer studentId;

    @PostConstruct()
    public Integer getStudentId() {
        return this.studentId;
    }

}
