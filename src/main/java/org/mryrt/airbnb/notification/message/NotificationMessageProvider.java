package org.mryrt.airbnb.notification.message;

import org.mryrt.airbnb.notification.model.NotificationType;

import java.util.Map;

public interface NotificationMessageProvider {

    NotificationContent getContent(NotificationType type, Map<String, Object> context);
}
