package com.isldevs.portfolio.content.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isldevs.portfolio.content.dto.ContentResponse;
import com.isldevs.portfolio.content.entity.ContentItem;
import java.util.Map;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class ContentMapper {
  @Autowired protected ObjectMapper objectMapper;

  public JsonNode json(Map<String, Object> value) {
    return objectMapper.valueToTree(value);
  }

  public abstract ContentResponse toDto(ContentItem item);
}
