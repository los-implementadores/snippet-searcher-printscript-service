package org.snippetsearcher.printscript.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origins allowed to call the API from a browser. Empty when the UI goes through the proxy. */
@ConfigurationProperties("cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
  }
}
