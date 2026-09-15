package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedNativeQueries;

@Entity
@NamedNativeQueries({@NamedNativeQuery(name = "User.findAll", query = "SELECT * FROM USER"), @NamedNativeQuery(name = "User.findById", query = "SELECT * FROM USER WHERE ID = ?", resultClass = NamedNativeQueriesOnValidClass.class)})
public class NamedNativeQueriesOnValidClass {
    @Id
    private Long id;
}
