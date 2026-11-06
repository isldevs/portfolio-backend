package com.isldevs.portfolio.entity.contact;

import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ContactMapper {
  Message toDto(Contact message);
}
