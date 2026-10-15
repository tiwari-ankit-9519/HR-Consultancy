package com.hrconsultancy.users.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http)
                        throws Exception {

                return http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/actuator/health",
                                                                "/actuator/health/**")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/users/*/audit-logs")
                                                .hasAnyRole(
                                                                "HR_ADMIN",
                                                                "HR_MANAGER",
                                                                "RECRUITER")

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/v1/users")
                                                .hasRole("HR_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/users")
                                                .hasAnyRole("HR_ADMIN", "HR_MANAGER")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/users/*")
                                                .hasAnyRole("HR_ADMIN", "HR_MANAGER")

                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt
                                                                .jwtAuthenticationConverter(
                                                                                jwtAuthenticationConverter())))
                                .build();
        }

        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

                converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                        Collection<GrantedAuthority> authorities = new ArrayList<>();

                        Collection<GrantedAuthority> scopes = scopeConverter.convert(jwt);

                        if (scopes != null) {
                                authorities.addAll(scopes);
                        }

                        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

                        if (realmAccess != null) {
                                Object rolesObject = realmAccess.get("roles");

                                if (rolesObject instanceof List<?> roles) {
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

                return converter;
        }
}