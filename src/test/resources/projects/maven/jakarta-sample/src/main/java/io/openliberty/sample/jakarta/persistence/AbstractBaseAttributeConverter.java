package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.AttributeConverter;

// Abstract base that implements AttributeConverter — used to test inherited implementation.
public abstract class AbstractBaseAttributeConverter implements AttributeConverter<String, Integer> {

    @Override
    public Integer convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : attribute.length();
    }

    @Override
    public String convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : String.valueOf(dbData);
    }
}
