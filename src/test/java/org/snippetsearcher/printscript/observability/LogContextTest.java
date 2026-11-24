package org.snippetsearcher.printscript.observability;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogContextTest {

  private static final String UUID_PATTERN =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  @Autowired private MockMvc mvc;

  @Test
  void keepsTheCallersCorrelationId() throws Exception {
    mvc.perform(get("/test/log-context").with(jwt()).header(CorrelationId.HEADER, "7f3a-91c2"))
        .andExpect(status().isOk())
        .andExpect(header().string(CorrelationId.HEADER, "7f3a-91c2"))
        .andExpect(jsonPath("$.correlationId").value("7f3a-91c2"));
  }

  @Test
  void startsACorrelationIdWhenThereIsNone() throws Exception {
    mvc.perform(get("/test/log-context").with(jwt()))
        .andExpect(header().string(CorrelationId.HEADER, matchesPattern(UUID_PATTERN)))
        .andExpect(jsonPath("$.correlationId").value(matchesPattern(UUID_PATTERN)));
  }

  @Test
  void replacesACorrelationIdThatDoesNotLookLikeOne() throws Exception {
    mvc.perform(
            get("/test/log-context").with(jwt()).header(CorrelationId.HEADER, "fake\" level=ERROR"))
        .andExpect(header().string(CorrelationId.HEADER, matchesPattern(UUID_PATTERN)));
  }

  @Test
  void rejectedRequestsGetACorrelationIdToo() throws Exception {
    mvc.perform(get("/test/log-context"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(CorrelationId.HEADER, matchesPattern(UUID_PATTERN)));
  }

  @Test
  void addsTheTokenSubjectAsUserId() throws Exception {
    mvc.perform(get("/test/log-context").with(jwt().jwt(token -> token.subject("auth0|ana"))))
        .andExpect(jsonPath("$.userId").value("auth0|ana"));
  }
}
