package edu.ecom.gateway.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import lombok.SneakyThrows;

public class JTIExtractor {

  @SneakyThrows
  public static String getJTI(String jwtToken) {
    String[] parts = jwtToken.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT token");
    }

    // Decode payload
    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

    // Parse JSON to Map
    ObjectMapper objectMapper = new ObjectMapper();
    return (String) objectMapper.readValue(payload, Map.class).get("jti");
  }
}
