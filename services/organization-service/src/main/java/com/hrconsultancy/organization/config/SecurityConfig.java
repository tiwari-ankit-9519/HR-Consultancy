
package com.hrconsultancy.organization.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize

                        // Public health endpoints
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info")
                        .permitAll()

                        // Read organizations and audit history
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/organizations",
                                "/api/v1/organizations/*",
                                "/api/v1/organizations/*/audit-logs")
                        .hasAnyRole(
                                "HR_ADMIN",
                                "HR_MANAGER",
                                "RECRUITER")

                        // Create an organization
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/organizations")
                        .hasAnyRole("HR_ADMIN", "HR_MANAGER")

                        // Update an organization
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/organizations/*")
                        .hasAnyRole("HR_ADMIN", "HR_MANAGER")

                        // Only HR administrators can deactivate
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/organizations/*")
                        .hasRole("HR_ADMIN")

                        // Other endpoints still require authentication
                        .anyRequest()
                        .authenticated())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter scopeConverter =
                new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {

            Collection<GrantedAuthority> authorities =
                    new ArrayList<>();

            // Preserve standard SCOPE_ authorities, if present.
            Collection<GrantedAuthority> scopeAuthorities =
                    scopeConverter.convert(jwt);

            if (scopeAuthorities != null) {
                authorities.addAll(scopeAuthorities);
            }

            // Read Keycloak realm roles from realm_access.roles.
            Map<String, Object> realmAccess =
                    jwt.getClaim("realm_access");

            if (realmAccess != null) {
                Object rolesClaim = realmAccess.get("roles");

                if (rolesClaim instanceof Collection<?> roles) {
                    for (Object role : roles) {
                        if (role instanceof String roleName) {
                            authorities.add(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + roleName));
                        }
                    }
                }
            }

            return authorities;
        });

        return authenticationConverter;
    }
}
