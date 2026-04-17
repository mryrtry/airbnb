package org.mryrt.airbnb.auth.config;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.jaas.XmlCredentialsStore;
import org.mryrt.airbnb.auth.model.Role;
import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.auth.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final XmlCredentialsStore xmlCredentialsStore;

    @EventListener(ApplicationReadyEvent.class)
    public void initAdmin() {
        String encodedPassword = passwordEncoder.encode("password");

        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(encodedPassword)
                    .roles(Set.of(Role.ROLE_USER, Role.ROLE_ADMIN))
                    .build();
            userRepository.save(admin);
        }

        // Always sync admin credentials to XML so JAAS can authenticate
        Set<String> roleNames = Set.of(Role.ROLE_USER, Role.ROLE_ADMIN).stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
        xmlCredentialsStore.addOrUpdateUser("admin", encodedPassword, roleNames);
    }

}
