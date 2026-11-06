package com.isldevs.portfolio.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import io.swagger.v3.core.jackson.ModelResolver;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One naming policy shared by HTTP JSON, dynamic metadata, and OpenAPI schemas. */
@Configuration(proxyBeanMethods = false)
public class JacksonConfiguration {
  public static final SnakeCaseStrategy API_NAMING = SnakeCaseStrategy.INSTANCE;

  @Bean
  Jackson2ObjectMapperBuilderCustomizer apiJsonNaming() {
    return builder -> builder.propertyNamingStrategy(API_NAMING);
  }

  @Bean
  ModelResolver apiSchemaResolver(ObjectMapper objectMapper, ObjectMapperProvider schemaMappers) {
    // Keep Swagger's schema modules while sharing the application's global naming policy.
    ObjectMapper schemaMapper = schemaMappers.jsonMapper().copy();
    schemaMapper.setPropertyNamingStrategy(objectMapper.getPropertyNamingStrategy());
    return new ModelResolver(schemaMapper).openapi31(schemaMappers.isOpenapi31());
  }
}
