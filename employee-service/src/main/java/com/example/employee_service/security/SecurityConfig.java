package com.example.employee_service.security;

import com.example.employee_service.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // abilita @PreAuthorize
public class SecurityConfig {

    @Autowired private JwtAuthFilter jwtAuthFilter;
    @Autowired private UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .authorizeHttpRequests(auth -> auth
                // Pagine pubbliche
                .requestMatchers("/auth/register", "/auth/login").permitAll()
                .requestMatchers("/", "/login.html", "/index.html", "/logo.jpg").permitAll()
                // GET: accessibili a USER e ADMIN
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/impiegati/**").hasAnyRole("USER", "ADMIN")
                // POST, PUT, PATCH, DELETE: solo ADMIN
                .requestMatchers(org.springframework.http.HttpMethod.POST,   "/impiegati/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT,    "/impiegati/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH,  "/impiegati/**").hasRole("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/impiegati/**").hasRole("ADMIN")
                // Tutto il resto richiede autenticazione
                .anyRequest().authenticated()
            )
            .sessionManagement(s -> s
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String accept = request.getHeader("Accept");
                    if (accept != null && accept.contains("text/html")) {
                        response.sendRedirect("/login.html");
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Non autorizzato");
                    }
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
