package org.mryrt.airbnb.auth.config;

import lombok.RequiredArgsConstructor;
import org.mryrt.airbnb.auth.model.Role;
import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.auth.repository.UserRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;


@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void initAdmin() {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("password"))
                    .roles(Set.of(Role.ROLE_USER, Role.ROLE_ADMIN))
                    .build();
            userRepository.save(admin);
        }
    }

}