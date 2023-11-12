package com.learning.restapi;

import com.learning.restapi.api.service.JwtVerificationService;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtVerificationService jwtVerificationService;
    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    public SecurityConfig(JwtVerificationService jwtVerificationService) {
        this.jwtVerificationService = jwtVerificationService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .addFilterBefore((Filter) jwtTokenFilter, BasicAuthenticationFilter.class) // Add the JwtTokenFilter
                .authorizeRequests()
                .anyRequest().permitAll();
        return http.build();
    }
}
