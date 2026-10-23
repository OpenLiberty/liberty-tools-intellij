package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedNativeQueries;

@NamedNativeQueries({@NamedNativeQuery(name = "User.findAll", query = "SELECT * FROM USER"), @NamedNativeQuery(name = "User.findById", query = "SELECT * FROM USER WHERE ID = ?", resultClass = NamedNativeQueriesOnInvalidClass.class)})
public class NamedNativeQueriesOnInvalidClass {
}
