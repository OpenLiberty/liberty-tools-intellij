package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ConverterWithAttributeConverter implements AttributeConverter<String, Integer> {

    @Override
    public Integer convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : attribute.length();
    }

    @Override
    public String convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : String.valueOf(dbData);
    }
}
