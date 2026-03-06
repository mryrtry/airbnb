package org.mryrt.airbnb.listing.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mryrt.airbnb.listing.model.Listing;

@Mapper(componentModel = "spring")
public interface ListingMapper {

    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "updatedAt", source = "lastModifiedDate")
    ListingDto toDto(Listing listing);
}
