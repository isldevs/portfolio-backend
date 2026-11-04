package com.isldevs.portfolio.content;

import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ContentMapper {
  ContentDto toDto(ContentItem item);
}
