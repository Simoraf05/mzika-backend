package com.mzika.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SessionConfig {

    @Bean
    public OncePerRequestFilter sameSiteFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {
                filterChain.doFilter(request, response);

                // Add SameSite=None to session cookie
                String header = response.getHeader("Set-Cookie");
                if (header != null && header.contains("JSESSIONID")) {
                    response.setHeader("Set-Cookie",
                            header + "; SameSite=None; Secure=false");
                }
            }
        };
    }
}