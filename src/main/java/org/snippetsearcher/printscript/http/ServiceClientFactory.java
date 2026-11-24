package org.snippetsearcher.printscript.http;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link RestClient}s used to call other Snippet Searcher services. They forward the
 * caller's JWT, so the next service authorizes the same user, and the correlation id, so both
 * services' logs share it.
 *
 * <p>For third-party APIs (Auth0's Management API, for example) inject a plain {@code
 * RestClient.Builder} instead: the user's token must not leave the platform.
 */
@Component
public class ServiceClientFactory {

  private final RestClient.Builder builder;

  public ServiceClientFactory(RestClient.Builder builder) {
    this.builder = builder;
  }

  public RestClient create(String baseUrl) {
    return builder
        .clone()
        .baseUrl(baseUrl)
        .requestInterceptor(new ContextPropagationInterceptor())
        .build();
  }
}
