package io.openliberty.sample.jakarta.persistence;

import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.TemporalType;

@Entity
public class MapKeyTemporalInvalid {

    @Id
    private Long id;

    // Invalid: map key is String
    @ElementCollection
    @MapKeyTemporal(TemporalType.DATE)
    private Map<String, String> stringEvents;

    // Invalid: map key is Integer
    @ElementCollection
    @MapKeyTemporal(TemporalType.DATE)
    private Map<Integer, String> integerEvents;

    // Invalid: getter with String key
    @ElementCollection
    @MapKeyTemporal(TemporalType.DATE)
    public Map<String, String> getStringEvents() {
        return this.stringEvents;
    }

    // Invalid: getter with Integer key
    @ElementCollection
    @MapKeyTemporal(TemporalType.TIMESTAMP)
    public Map<Integer, String> getIntegerEvents() {
        return this.integerEvents;
    }
}
