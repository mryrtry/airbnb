package org.mryrt.airbnb.notification.dto;

import org.mapstruct.Mapper;
import org.mryrt.airbnb.notification.model.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);
}
