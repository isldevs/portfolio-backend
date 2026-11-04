package com.isldevs.portfolio.common;

import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditMapper {
  AuditDto toDto(AuditLog log);
}
