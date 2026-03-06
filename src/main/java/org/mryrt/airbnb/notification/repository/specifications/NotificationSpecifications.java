package org.mryrt.airbnb.notification.repository.specifications;

import org.mryrt.airbnb.notification.model.Notification;
import org.mryrt.airbnb.notification.repository.filter.NotificationFilter;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecifications {

    public static Specification<Notification> withFilter(NotificationFilter filter) {
        if (filter == null) return null;

        return Specification.<Notification>where(
                        filter.getUserId() == null ? null : (root, q, cb) -> cb.equal(root.get("userId"), filter.getUserId()))
                .and(filter.getUnreadOnly() == null || !filter.getUnreadOnly() ? null : (root, q, cb) -> cb.equal(root.get("read"), false))
                .and(filter.getType() == null ? null : (root, q, cb) -> cb.equal(root.get("type"), filter.getType()));
    }
}
