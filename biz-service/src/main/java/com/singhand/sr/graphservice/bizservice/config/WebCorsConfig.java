package com.singhand.sr.graphservice.bizservice.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class WebCorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {

    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
    configuration.setAllowedMethods(List.of(CorsConfiguration.ALL));
    configuration.setAllowCredentials(true);
    configuration.setAllowedOriginPatterns(List.of(CorsConfiguration.ALL));
    configuration.addExposedHeader("x-amz-meta-username");
    configuration.addExposedHeader("x-amz-meta-status");
    configuration.addExposedHeader("x-amz-meta-retries");
    configuration.addExposedHeader("x-amz-meta-origin-filename");
    configuration.setMaxAge(5000L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}