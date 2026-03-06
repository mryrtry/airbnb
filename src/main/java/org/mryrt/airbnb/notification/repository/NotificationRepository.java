package org.mryrt.airbnb.notification.repository;

import org.mryrt.airbnb.notification.model.Notification;
import org.mryrt.airbnb.notification.repository.filter.NotificationFilter;
import org.mryrt.airbnb.notification.repository.specifications.NotificationSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    default Page<Notification> findWithFilter(NotificationFilter filter, Pageable pageable) {
        Specification<Notification> spec = NotificationSpecifications.withFilter(filter);
        return findAll(spec, pageable);
    }
}
