package com.smartinventory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * <p>
 * Note: CORS is centrally configured in {@link com.smartinventory.security.config.SecurityConfig}
 * via {@link org.springframework.web.cors.CorsConfigurationSource} to avoid duplicate or conflicting
 * CORS processing between Spring Security's CorsFilter and Spring WebMvc.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
}
