package io.openliberty.sample.jakarta.security.identitystore;

import jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition;

@LdapIdentityStoreDefinition(
    url = "ldap://localhost:10389",
    callerBaseDn = "ou=caller,dc=jsr375,dc=net",
    groupSearchBase = "ou=group,dc=jsr375,dc=net"
)
public class LdapIdentityStoreMissingApplicationScoped {
    // Invalid: Missing @ApplicationScoped annotation
}