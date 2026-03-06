package org.mryrt.airbnb.notification.config;

import org.mryrt.airbnb.notification.model.Notification;
import org.mryrt.airbnb.util.pageable.sort.SortConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class NotificationSortConfig implements SortConfig {

    @Override
    public Class<?> getEntityClass() {
        return Notification.class;
    }

    @Override
    public Set<String> getAllowedSortFields() {
        return Set.of("id", "userId", "type", "read", "createdAt", "readAt");
    }

    @Override
    public String getDefaultSortField() {
        return "createdAt";
    }
}
