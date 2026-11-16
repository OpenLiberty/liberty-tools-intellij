package io.openliberty.sample.jakarta.security.identitystore;

import jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

@DatabaseIdentityStoreDefinition(
    dataSourceLookup = "java:comp/DefaultDataSource",
    callerQuery = "select password from caller where name = ?",
    groupsQuery = "select group_name from caller_groups where caller_name = ?"
)
public class DatabaseIdentityStoreMissingApplicationScoped {
    // Invalid: Missing @ApplicationScoped annotation
}