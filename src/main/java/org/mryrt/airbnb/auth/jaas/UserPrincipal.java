package org.mryrt.airbnb.auth.jaas;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

public final class UserPrincipal implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String name;

    public UserPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
