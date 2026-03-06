package org.mryrt.airbnb.auth.config;

import org.mryrt.airbnb.auth.model.User;
import org.mryrt.airbnb.util.pageable.sort.SortConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Set;


@Configuration
public class UserSortConfig implements SortConfig {

    @Override
    public Class<?> getEntityClass() {
        return User.class;
    }

    @Override
    public Set<String> getAllowedSortFields() {
        return Set.of("id", "username", "email", "createdAt", "updatedAt");
    }

    @Override
    public String getDefaultSortField() {
        return "id";
    }

}
