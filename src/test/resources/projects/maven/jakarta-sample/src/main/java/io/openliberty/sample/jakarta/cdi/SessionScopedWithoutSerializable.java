package io.openliberty.sample.jakarta.cdi;

import jakarta.enterprise.context.SessionScoped;

// Invalid: @SessionScoped requires Serializable
@SessionScoped
public class SessionScopedWithoutSerializable {
}
