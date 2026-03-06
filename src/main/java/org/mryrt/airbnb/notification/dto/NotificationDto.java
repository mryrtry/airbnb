package org.mryrt.airbnb.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.mryrt.airbnb.notification.model.NotificationType;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private Long id;
    private Long userId;
    private NotificationType type;
    private String title;
    private String body;
    private Long relatedBookingId;
    private Long relatedResolutionId;
    private boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
