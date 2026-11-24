package org.snippetsearcher.printscript.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs one line per request with its final status and duration: 5xx as error, 4xx as warning. The
 * query string is left out because it may carry data that shouldn't end up in the logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLogFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestLogFilter.class);

  /**
   * Health checks run every few seconds (Swarm, the proxy): logging them buries everything else.
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/actuator/health");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    long start = System.nanoTime();
    boolean failed = true;
    try {
      chain.doFilter(request, response);
      failed = false;
    } finally {
      // An exception that escapes the chain only becomes a 500 later on, while the response still
      // says 200: log the status the client will actually get.
      int status = failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();
      log(request, status, (System.nanoTime() - start) / 1_000_000);
    }
  }

  private static void log(HttpServletRequest request, int status, long millis) {
    String format = "{} {} -> {} ({} ms)";
    if (status >= 500) {
      LOGGER.error(format, request.getMethod(), request.getRequestURI(), status, millis);
    } else if (status >= 400) {
      LOGGER.warn(format, request.getMethod(), request.getRequestURI(), status, millis);
    } else {
      LOGGER.info(format, request.getMethod(), request.getRequestURI(), status, millis);
    }
  }
}
