package com.isldevs.portfolio.content.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isldevs.portfolio.common.json.ApiJsonNames;
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

  @Named("document")
  public JsonNode json(Map<String, Object> value) {
    return objectMapper.valueToTree(value);
  }

  public JsonNode wireData(Map<String, Object> value) {
    return objectMapper.valueToTree(ApiJsonNames.toWire(value));
  }

  @Mapping(target = "body", qualifiedByName = "document")
  @Mapping(target = "data", expression = "java(wireData(item.data))")
  public abstract ContentResponse toDto(ContentItem item);
}
