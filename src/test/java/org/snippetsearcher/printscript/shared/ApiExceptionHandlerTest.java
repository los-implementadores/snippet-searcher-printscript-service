package org.snippetsearcher.printscript.shared;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiExceptionHandlerTest {

  @Autowired private MockMvc mvc;

  @ParameterizedTest
  @CsvSource({
    "not-found, 404, Snippet not found",
    "forbidden, 403, Only the owner can do this",
    "bad-request, 400, Unsupported language",
    "downstream, 502, Permissions service failed"
  })
  void mapsDomainExceptionsToProblems(String kind, int status, String detail) throws Exception {
    mvc.perform(get("/test/errors/{kind}", kind).with(jwt()))
        .andExpect(status().is(status))
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(status))
        .andExpect(jsonPath("$.detail").value(detail));
  }

  @Test
  void springErrorsUseTheSameFormat() throws Exception {
    mvc.perform(get("/does-not-exist").with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
