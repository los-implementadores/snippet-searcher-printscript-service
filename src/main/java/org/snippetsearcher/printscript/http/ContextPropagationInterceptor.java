package org.snippetsearcher.printscript.http;

import java.io.IOException;
import org.snippetsearcher.printscript.observability.CorrelationId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Copies the caller's JWT and the correlation id of the current request onto an outgoing request.
 * An Authorization header set by the caller is kept. Outside a request (a stream consumer, a
 * scheduled job) there is neither, so nothing is added.
 */
class ContextPropagationInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    HttpHeaders headers = request.getHeaders();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication
        && !headers.containsHeader(HttpHeaders.AUTHORIZATION)) {
      headers.setBearerAuth(jwtAuthentication.getToken().getTokenValue());
    }
    CorrelationId.current().ifPresent(id -> headers.set(CorrelationId.HEADER, id));
    return execution.execute(request, body);
  }
}
