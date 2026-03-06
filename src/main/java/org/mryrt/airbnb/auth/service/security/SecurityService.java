package org.mryrt.airbnb.auth.service.security;

public interface SecurityService {

    boolean hasPermission(String permission);

    boolean hasAnyPermission(String... permissions);

    boolean hasAllPermissions(String... permissions);

}
