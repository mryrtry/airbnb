package org.mryrt.airbnb.listing.config;

import org.mryrt.airbnb.listing.model.Listing;
import org.mryrt.airbnb.util.pageable.sort.SortConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class ListingSortConfig implements SortConfig {

    @Override
    public Class<?> getEntityClass() {
        return Listing.class;
    }

    @Override
    public Set<String> getAllowedSortFields() {
        return Set.of("id", "ownerId", "title", "status", "createdDate", "lastModifiedDate");
    }

    @Override
    public String getDefaultSortField() {
        return "id";
    }
}
