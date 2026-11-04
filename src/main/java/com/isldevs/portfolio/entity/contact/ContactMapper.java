package com.isldevs.portfolio.entity.contact;

import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ContactMapper {
  ContactService.Message toDto(ContactMessage message);
}
