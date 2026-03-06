package org.mryrt.airbnb.resolution.config;

import org.mryrt.airbnb.resolution.model.ResolutionWindow;
import org.mryrt.airbnb.util.pageable.sort.SortConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ResolutionSortConfig implements SortConfig {

    @Override
    public Class<?> getEntityClass() {
        return ResolutionWindow.class;
    }

    @Override
    public Set<String> getAllowedSortFields() {
        return Set.of("id", "bookingId", "status", "openedAt", "closedAt", "createdDate", "lastModifiedDate");
    }

    @Override
    public String getDefaultSortField() {
        return "openedAt";
    }
}
