package io.openliberty.sample.jakarta.persistence;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.TemporalType;

@Entity
public class MapKeyTemporalValid {

    @Id
    private Long id;

    // Valid: map key is Date
    @ElementCollection
    @MapKeyTemporal(TemporalType.DATE)
    private Map<Date, String> dateEvents;

    // Valid: map key is Calendar
    @ElementCollection
    @MapKeyTemporal(TemporalType.TIMESTAMP)
    private Map<Calendar, String> calendarEvents;

    // Valid: getter with Date key
    @ElementCollection
    @MapKeyTemporal(TemporalType.DATE)
    public Map<Date, String> getDateEvents() {
        return this.dateEvents;
    }

    // Valid: getter with Calendar key
    @ElementCollection
    @MapKeyTemporal(TemporalType.TIMESTAMP)
    public Map<Calendar, String> getCalendarEvents() {
        return this.calendarEvents;
    }
}
