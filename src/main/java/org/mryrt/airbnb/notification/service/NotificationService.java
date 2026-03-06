package org.mryrt.airbnb.notification.service;

import org.mryrt.airbnb.notification.dto.NotificationDto;
import org.mryrt.airbnb.notification.model.NotificationType;
import org.mryrt.airbnb.notification.repository.filter.NotificationFilter;
import org.mryrt.airbnb.util.pageable.PageableRequest;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface NotificationService {

    void notifyUser(Long userId, NotificationType type, String title, String body);

    void notifyUser(Long userId, NotificationType type, Map<String, Object> context);

    Page<NotificationDto> getAll(NotificationFilter filter, PageableRequest pageable);

    NotificationDto get(Long id);

    NotificationDto markRead(Long id);

    void markAllRead(Long userId);

    long countUnread(Long userId);
}
