package org.mryrt.airbnb.auth.model;

import lombok.Getter;

import java.util.Set;

@Getter
public enum Role {
    ROLE_USER(Set.of(
            Permission.LISTING_READ,
            Permission.BOOKING_READ,
            Permission.BOOKING_CREATE,
            Permission.RESOLUTION_READ,
            Permission.RESOLUTION_RESPOND,
            Permission.NOTIFICATION_READ
    )),
    ROLE_OWNER(Set.of(
            Permission.LISTING_CREATE,
            Permission.LISTING_UPDATE,
            Permission.LISTING_DELETE,
            Permission.BOOKING_DECIDE,
            Permission.RESOLUTION_OPEN,
            Permission.RESOLUTION_REQUEST_MONEY,
            Permission.RESOLUTION_ESCALATE,
            Permission.RESOLUTION_COMPLAINT
    )),
    ROLE_ADMIN(Set.of(
            Permission.ALL_USER_READ,
            Permission.ALL_USER_UPDATE,
            Permission.ALL_USER_DELETE,
            Permission.ALL_LISTING_READ,
            Permission.ALL_LISTING_UPDATE,
            Permission.ALL_LISTING_DELETE,
            Permission.ALL_BOOKING_READ,
            Permission.ALL_BOOKING_UPDATE,
            Permission.ALL_RESOLUTION_READ,
            Permission.ALL_RESOLUTION_UPDATE,
            Permission.ALL_NOTIFICATION_READ
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
