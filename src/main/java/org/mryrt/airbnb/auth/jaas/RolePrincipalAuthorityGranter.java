package org.mryrt.airbnb.auth.jaas;

import org.springframework.security.authentication.jaas.AuthorityGranter;

import java.security.Principal;
import java.util.Set;

public class RolePrincipalAuthorityGranter implements AuthorityGranter {

    @Override
    public Set<String> grant(Principal principal) {
        if (principal instanceof RolePrincipal) {
            return Set.of(principal.getName());
        }
        return null;
    }
}
