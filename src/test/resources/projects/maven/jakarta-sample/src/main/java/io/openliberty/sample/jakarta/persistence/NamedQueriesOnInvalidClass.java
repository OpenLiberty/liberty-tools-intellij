package io.openliberty.sample.jakarta.persistence;

import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedQueries;

@NamedQueries({@NamedQuery(name = "User.findAll", query = "SELECT u FROM User u"), @NamedQuery(name = "User.findById", query = "SELECT u FROM User u WHERE u.id = :id")})
public class NamedQueriesOnInvalidClass {
}
