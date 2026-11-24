package org.snippetsearcher.printscript.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

  @Autowired private MockMvc mvc;

  @Test
  void healthIsPublic() throws Exception {
    mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
  }

  @Test
  void everythingElseNeedsAToken() throws Exception {
    mvc.perform(get("/test/log-context")).andExpect(status().isUnauthorized());
  }

  @Test
  void aValidTokenGetsThrough() throws Exception {
    mvc.perform(get("/test/log-context").with(jwt())).andExpect(status().isOk());
  }

  @Test
  void allowsConfiguredOrigins() throws Exception {
    mvc.perform(preflightFrom("http://ui.test"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://ui.test"));
  }

  @Test
  void rejectsOtherOrigins() throws Exception {
    mvc.perform(preflightFrom("http://evil.test")).andExpect(status().isForbidden());
  }

  private static MockHttpServletRequestBuilder preflightFrom(String origin) {
    return options("/test/log-context")
        .header(HttpHeaders.ORIGIN, origin)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.AUTHORIZATION);
  }
}
