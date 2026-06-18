package io.openliberty.sample.jakarta.security.identitystore;

import jakarta.enterprise.context.RequestScoped;
import jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

@DatabaseIdentityStoreDefinition(
    dataSourceLookup = "java:comp/DefaultDataSource",
    callerQuery = "select password from caller where name = ?",
    groupsQuery = "select group_name from caller_groups where caller_name = ?"
)
@RequestScoped
public class DatabaseIdentityStoreWrongScope {
    // Invalid: Using @RequestScoped instead of @ApplicationScoped
}