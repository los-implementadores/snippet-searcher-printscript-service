package org.snippetsearcher.template;

import java.util.Map;
import org.slf4j.MDC;
import org.snippetsearcher.template.observability.CorrelationId;
import org.snippetsearcher.template.observability.UserMdcFilter;
import org.snippetsearcher.template.shared.BadRequestException;
import org.snippetsearcher.template.shared.DownstreamServiceException;
import org.snippetsearcher.template.shared.ForbiddenOperationException;
import org.snippetsearcher.template.shared.ResourceNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints that exist only in tests, to exercise the cross-cutting pieces without a domain. */
@RestController
@RequestMapping("/test")
class TestEndpoints {

  /** What the log context holds while the request is handled. */
  @GetMapping("/log-context")
  Map<String, String> logContext() {
    return Map.of(
        "correlationId", String.valueOf(MDC.get(CorrelationId.MDC_KEY)),
        "userId", String.valueOf(MDC.get(UserMdcFilter.MDC_KEY)));
  }

  @GetMapping("/errors/{kind}")
  void fail(@PathVariable String kind) {
    switch (kind) {
      case "not-found" -> throw new ResourceNotFoundException("Snippet not found");
      case "forbidden" -> throw new ForbiddenOperationException("Only the owner can do this");
      case "bad-request" -> throw new BadRequestException("Unsupported language");
      case "downstream" ->
          throw new DownstreamServiceException(
              "Permissions service failed", new RuntimeException());
      default -> throw new IllegalArgumentException("Unknown kind: " + kind);
    }
  }
}
