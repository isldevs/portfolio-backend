package com.isldevs.portfolio;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isldevs.portfolio.common.exception.CustomExceptionHandler;
import com.isldevs.portfolio.contact.controller.ContactController;
import com.isldevs.portfolio.contact.service.ContactRateLimiter;
import com.isldevs.portfolio.contact.service.ContactService;
import jakarta.persistence.Entity;
import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class ResponseContractTest {
  @Test
  void controllerResponseContractsNeverExposeMapsOrEntities() throws Exception {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
    var controllers = scanner.findCandidateComponents("com.isldevs.portfolio");
    assertThat(controllers).hasSize(6);
    for (var bean : controllers) {
      for (var method : Class.forName(bean.getBeanClassName()).getDeclaredMethods()) {
        if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class)) {
          assertTyped(method.getGenericReturnType(), new HashSet<>());
        }
      }
    }
  }

  private void assertTyped(Type type, Set<Type> visited) {
    if (!visited.add(type)) return;
    assertThat(type).isNotInstanceOf(WildcardType.class);
    if (type instanceof ParameterizedType parameterized) {
      assertTyped(parameterized.getRawType(), visited);
      for (Type argument : parameterized.getActualTypeArguments()) assertTyped(argument, visited);
    } else if (type instanceof Class<?> clazz) {
      assertThat(Map.class.isAssignableFrom(clazz)).as("Response type %s", clazz).isFalse();
      assertThat(clazz.isAnnotationPresent(Entity.class))
          .as("JPA entity in response: %s", clazz)
          .isFalse();
      if (clazz.isRecord() && clazz.getPackageName().startsWith("com.isldevs.portfolio")) {
        assertThat(clazz.isAnnotationPresent(JsonNaming.class))
            .as("DTO naming must be global")
            .isFalse();
        for (var component : clazz.getRecordComponents())
          assertTyped(component.getGenericType(), visited);
      }
    }
  }

  @Test
  void rateLimitRetainsProblemDetailsAndRetryHeader() throws Exception {
    var limiter = mock(ContactRateLimiter.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(new ContactController(mock(ContactService.class), limiter))
            .setControllerAdvice(new CustomExceptionHandler())
            .build();
    mvc.perform(
            post("/api/v1/public/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"Test Visitor\",\"email\":\"visitor@example.com\",\"subject\":\"Test message\",\"message\":\"A sufficiently long message.\",\"website\":\"\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "3600"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(429));
  }
}
