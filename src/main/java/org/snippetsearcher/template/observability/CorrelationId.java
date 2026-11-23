package org.snippetsearcher.template.observability;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * The id that follows one request across services: it travels in the {@code X-Correlation-Id}
 * header (the course's asset service reads the same one) and every log line carries it.
 */
public final class CorrelationId {

  public static final String HEADER = "X-Correlation-Id";
  public static final String MDC_KEY = "correlationId";

  private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  private CorrelationId() {}

  /** The id of the request being handled on this thread, if any. */
  public static Optional<String> current() {
    return Optional.ofNullable(MDC.get(MDC_KEY));
  }

  /**
   * Keeps the caller's id when it looks like one and starts a new one otherwise. Rejecting anything
   * else keeps arbitrary text (line breaks, for example) out of the logs.
   */
  static String reuseOrCreate(String incoming) {
    if (incoming != null && VALID.matcher(incoming).matches()) {
      return incoming;
    }
    return UUID.randomUUID().toString();
  }
}
