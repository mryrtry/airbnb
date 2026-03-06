package org.mryrt.airbnb.notification.message;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationContent {

    private final String title;
    private final String body;
}
