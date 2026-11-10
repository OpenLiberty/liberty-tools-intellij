package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyEnumerated;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
public class MapKeyEnumeratedInvalid {

    @Id
    private int id;

    // Invalid: map key is String, not an enum
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    private Map<String, String> stringKeyMap = new HashMap<>();

    // Invalid: map key is Integer, not an enum
    @ElementCollection
    @MapKeyEnumerated(EnumType.ORDINAL)
    private Map<Integer, String> intKeyMap = new HashMap<>();

    // Invalid: field type is List, not Map
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    private List<String> listField;

    // Invalid: raw Map — key type not verifiable as enum
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    private Map rawMap = new HashMap();

    // Invalid: wildcard key — cannot guarantee it is an enum
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    private Map<?, String> wildcardKeyMap;

    // Invalid: method return type is String, not Map
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    public String getStringField() {
        return null;
    }

    // Invalid: method returns Map with non-enum key
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    public Map<String, String> getStringKeyMap() {
        return this.stringKeyMap;
    }

    public MapKeyEnumeratedInvalid() {
    }
}
