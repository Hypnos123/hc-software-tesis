package com.krivi.apihistorialmedico.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {
  private static final String FRONTEND_LOCAL = "http://localhost:4200";

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
        .allowedOrigins(FRONTEND_LOCAL)
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("Accept", "Content-Type", "Origin", "X-Usuario-Id")
        .maxAge(3600);
  }
}
