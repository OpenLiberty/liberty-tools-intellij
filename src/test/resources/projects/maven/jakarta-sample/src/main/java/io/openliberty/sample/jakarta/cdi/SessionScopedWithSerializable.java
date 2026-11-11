package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.SessionScoped;
import java.io.Serializable;

// Valid: @SessionScoped with Serializable
@SessionScoped
public class SessionScopedWithSerializable implements Serializable {
}
