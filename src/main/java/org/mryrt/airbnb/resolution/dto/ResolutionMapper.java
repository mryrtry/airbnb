package org.mryrt.airbnb.resolution.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mryrt.airbnb.resolution.model.ResolutionWindow;

@Mapper(componentModel = "spring")
public interface ResolutionMapper {

    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "updatedAt", source = "lastModifiedDate")
    ResolutionDto toDto(ResolutionWindow window);
}
