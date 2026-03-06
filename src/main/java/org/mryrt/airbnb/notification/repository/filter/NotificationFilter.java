package org.mryrt.airbnb.notification.repository.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.notification.model.NotificationType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationFilter {

    private Long userId;
    private Boolean unreadOnly;
    private NotificationType type;
}
