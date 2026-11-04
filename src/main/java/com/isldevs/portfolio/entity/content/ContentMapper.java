package com.isldevs.portfolio.entity.content;

import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ContentMapper {
  ContentDto toDto(ContentItem item);
}
