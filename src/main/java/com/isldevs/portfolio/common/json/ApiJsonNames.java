package com.isldevs.portfolio.common.json;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.util.*;
import java.util.function.UnaryOperator;

/** API naming boundary; database keys and editor node protocols remain unchanged. */
public final class ApiJsonNames {
  private ApiJsonNames() {}

  public static String snake(String name) {
    return PropertyNamingStrategies.SnakeCaseStrategy.INSTANCE.translate(name);
  }

  public static String internal(String name) {
    StringBuilder result = new StringBuilder();
    boolean uppercase = false;
    for (char c : name.toCharArray()) {
      if (c == '_') uppercase = true;
      else {
        result.append(uppercase ? Character.toUpperCase(c) : c);
        uppercase = false;
      }
    }
    return result.toString();
  }

  public static Map<String, Object> toWire(Map<String, Object> value) {
    return map(value, ApiJsonNames::snake);
  }

  public static Map<String, Object> toInternal(Map<String, Object> value) {
    return map(value, ApiJsonNames::internal);
  }

  private static Map<String, Object> map(Map<String, Object> value, UnaryOperator<String> key) {
    if (value == null) return null;
    Map<String, Object> result = new LinkedHashMap<>();
    value.forEach(
        (name, item) -> {
          String converted = key.apply(name);
          if (result.containsKey(converted))
            throw new IllegalArgumentException("Duplicate normalized JSON field: " + converted);
          // Rich-text body follows the editor's document protocol, including camelCase attrs/type
          // values.
          result.put(converted, name.equals("body") ? item : convert(item, key));
        });
    return result;
  }

  private static Object convert(Object value, UnaryOperator<String> key) {
    if (value instanceof Map<?, ?> values) {
      Map<String, Object> typed = new LinkedHashMap<>();
      values.forEach((name, item) -> typed.put(String.valueOf(name), item));
      return map(typed, key);
    }
    if (value instanceof List<?> values)
      return values.stream().map(item -> convert(item, key)).toList();
    return value;
  }
}
