package org.openscience.sherlock.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    @Value("${spring.security.user.name}")
    private String username;

    @Value("${spring.security.user.password}")
    private String password;

    @Value("${springdoc.api-docs.path}")
    private String apiDoc;

    @Value("${springdoc.swagger-ui.path}")
    private String swaggerUi;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/").permitAll() // Public endpoint
                        .requestMatchers("/query").permitAll() // Public endpoint
                        .requestMatchers("/error").permitAll() // Public endpoint for error handling
                        .requestMatchers(apiDoc).permitAll() // Public endpoint for API documentation
                        .requestMatchers(apiDoc + "/**").permitAll() // Public endpoint for OpenAPI subpaths
                        .requestMatchers(swaggerUi).permitAll() // Public endpoint for Swagger UI
                        .requestMatchers("/swagger-ui/**").permitAll() // Public endpoint for Swagger UI assets
                        .anyRequest().authenticated() // All other endpoints require authentication
                )
                .httpBasic(Customizer.withDefaults()) // Enable HTTP Basic Authentication
                // Use stateless session management for REST APIs
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Create in-memory users
        UserDetails user = User.builder()
                .username(username)
                .password(passwordEncoder().encode(password))
                // .roles("ADMIN")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
