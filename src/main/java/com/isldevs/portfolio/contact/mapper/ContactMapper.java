package com.isldevs.portfolio.contact.mapper;

import com.isldevs.portfolio.contact.dto.MessageResponse;
import com.isldevs.portfolio.contact.entity.Contact;
import org.mapstruct.*;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ContactMapper {
  MessageResponse toDto(Contact message);
}
