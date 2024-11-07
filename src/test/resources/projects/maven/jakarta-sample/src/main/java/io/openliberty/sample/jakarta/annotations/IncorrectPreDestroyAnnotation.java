package io.openliberty.sample.jakarta.annotations;

import random.test.pkg.on.PreDestroy;
import jakarta.annotation.Resource;

@Resource(type = Object.class, name = "aa") 
public class IncorrectPreDestroyAnnotation {

    private Integer studentId;
	
    private boolean isHappy;

    private boolean isSad;
	
	@PreDestroy()
	public Integer getStudentId() {
		return this.studentId;
	}
	
	@random.test.pkg.on.PreDestroy()
	public boolean getHappiness(String type) {
		if (type.equals("happy")) return this.isHappy;
		return this.isSad;
	}
}



