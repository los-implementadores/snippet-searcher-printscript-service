package org.snippetsearcher.printscript.auth;

import java.util.List;
import org.snippetsearcher.printscript.observability.CorrelationId;
import org.snippetsearcher.printscript.observability.UserMdcFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Every endpoint except health requires a bearer JWT issued by the OAuth provider (Auth0). The
 * issuer and audience are configured through {@code spring.security.oauth2.resourceserver.jwt.*}.
 * The token subject is the user id used across services.
 *
 * <p>Scopes are checked here ({@code hasAuthority("SCOPE_read:snippets")}); who can do what on each
 * resource (owner, shared with) is domain logic and belongs in the services.
 */
@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
        .addFilterAfter(new UserMdcFilter(), BearerTokenAuthenticationFilter.class)
        .build();
  }

  /** Only matters when the UI calls the service from another origin (the Vite dev server). */
  @Bean
  CorsConfigurationSource corsConfigurationSource(CorsProperties cors) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(cors.allowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, CorrelationId.HEADER));
    config.setExposedHeaders(List.of(CorrelationId.HEADER, HttpHeaders.CONTENT_DISPOSITION));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
