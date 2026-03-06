package org.mryrt.airbnb.booking.config;

import org.mryrt.airbnb.booking.model.Booking;
import org.mryrt.airbnb.util.pageable.sort.SortConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class BookingSortConfig implements SortConfig {

    @Override
    public Class<?> getEntityClass() {
        return Booking.class;
    }

    @Override
    public Set<String> getAllowedSortFields() {
        return Set.of("id", "listingId", "guestId", "status", "appliedAt", "checkInAt", "checkOutAt", "createdDate", "lastModifiedDate");
    }

    @Override
    public String getDefaultSortField() {
        return "appliedAt";
    }
}
