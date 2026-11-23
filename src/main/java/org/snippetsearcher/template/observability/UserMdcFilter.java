package org.snippetsearcher.template.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts the caller's user id (the token subject) in the log context. It runs inside the security
 * chain, right after the bearer token is authenticated (see {@code SecurityConfig}). It isn't a
 * {@code @Component} on purpose: Spring Boot would also run it outside the chain, before there is a
 * user.
 */
public class UserMdcFilter extends OncePerRequestFilter {

  public static final String MDC_KEY = "userId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      MDC.put(MDC_KEY, jwtAuthentication.getName());
    }
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
