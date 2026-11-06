package com.isldevs.portfolio.audit.mapper;

import com.isldevs.portfolio.audit.dto.AuditResponse;
import com.isldevs.portfolio.audit.entity.AuditLog;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditMapper {
  AuditResponse toDto(AuditLog log);
}
