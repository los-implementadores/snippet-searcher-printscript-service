package org.snippetsearcher.template.http;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.snippetsearcher.template.observability.CorrelationId;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ServiceClientFactoryTest {

  private MockRestServiceServer server;
  private RestClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new ServiceClientFactory(builder).create("http://permissions.test");
  }

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  void forwardsTheCallersTokenAndCorrelationId() {
    authenticateWithToken("user-token");
    MDC.put(CorrelationId.MDC_KEY, "7f3a-91c2");
    server
        .expect(requestTo("http://permissions.test/permissions"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
        .andExpect(header(CorrelationId.HEADER, "7f3a-91c2"))
        .andRespond(withSuccess());

    client.get().uri("/permissions").retrieve().toBodilessEntity();

    server.verify();
  }

  @Test
  void keepsAnAuthorizationHeaderSetByTheCaller() {
    authenticateWithToken("user-token");
    server
        .expect(requestTo("http://permissions.test/permissions"))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer machine-token"))
        .andRespond(withSuccess());

    client
        .get()
        .uri("/permissions")
        .headers(headers -> headers.setBearerAuth("machine-token"))
        .retrieve()
        .toBodilessEntity();

    server.verify();
  }

  @Test
  void addsNothingOutsideARequest() {
    server
        .expect(requestTo("http://permissions.test/permissions"))
        .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
        .andExpect(headerDoesNotExist(CorrelationId.HEADER))
        .andRespond(withSuccess());

    client.get().uri("/permissions").retrieve().toBodilessEntity();

    server.verify();
  }

  private static void authenticateWithToken(String tokenValue) {
    Jwt jwt = Jwt.withTokenValue(tokenValue).header("alg", "none").subject("auth0|ana").build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
  }
}
