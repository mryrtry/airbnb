package org.mryrt.airbnb.booking.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mryrt.airbnb.booking.model.Booking;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "updatedAt", source = "lastModifiedDate")
    BookingDto toDto(Booking booking);
}
