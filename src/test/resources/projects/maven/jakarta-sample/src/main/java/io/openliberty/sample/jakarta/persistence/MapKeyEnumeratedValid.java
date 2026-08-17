package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyEnumerated;

import java.util.HashMap;
import java.util.Map;

@Entity
public class MapKeyEnumeratedValid {

    @Id
    private int id;

    // Valid: map key is an enum
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    private Map<RoleType, String> statusMap = new HashMap<>();

    // Valid: map key is an enum (ORDINAL)
    @ElementCollection
    @MapKeyEnumerated(EnumType.ORDINAL)
    private Map<RoleType, Integer> statusOrdinalMap = new HashMap<>();

    // Valid: method returns Map with enum key
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    public Map<RoleType, String> getRoleTypeMap() {
        return this.statusMap;
    }

    // Valid: upper-bounded wildcard whose bound is an enum
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    private Map<? extends RoleType, String> boundedWildcardMap;

    public MapKeyEnumeratedValid() {
    }
}
