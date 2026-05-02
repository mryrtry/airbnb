package org.mryrt.airbnb.auth.config;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.jaas.XmlCredentialsStore;
import org.mryrt.airbnb.auth.model.Role;
import org.mryrt.airbnb.auth.model.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;


@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private static final Set<Role> ADMIN_ROLES = Set.of(Role.ROLE_USER, Role.ROLE_OWNER, Role.ROLE_ADMIN);

    private final PasswordEncoder passwordEncoder;

    private final XmlCredentialsStore xmlCredentialsStore;

    @EventListener(ApplicationReadyEvent.class)
    public void initAdmin() {
        xmlCredentialsStore.findByUsername("admin").ifPresentOrElse(
                this::syncAdminRoles,
                this::createAdmin
        );
    }

    private void createAdmin() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRoles(ADMIN_ROLES);
        xmlCredentialsStore.saveUser(admin);
    }

    private void syncAdminRoles(User admin) {
        if (ADMIN_ROLES.equals(admin.getRoles())) return;
        admin.setRoles(ADMIN_ROLES);
        xmlCredentialsStore.saveUser(admin);
    }

}
