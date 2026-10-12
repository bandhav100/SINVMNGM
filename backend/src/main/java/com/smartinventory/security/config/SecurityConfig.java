package com.smartinventory.security.config;

import com.smartinventory.security.handler.CustomAccessDeniedHandler;
import com.smartinventory.security.handler.CustomAuthenticationEntryPoint;
import com.smartinventory.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/setup-admin",
                                "/api/v1/auth/setup-status",
                                "/api/v1/auth/csrf",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/actuator/health",
                                "/error"
                        )
                        .ignoringRequestMatchers(request -> {
                            // API clients using Authorization Bearer token are immune to CSRF
                            String authHeader = request.getHeader("Authorization");
                            return authHeader != null && authHeader.startsWith("Bearer ");
                        })
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public Endpoints
                        .requestMatchers(
                                "/api/v1/auth/setup-status",
                                "/api/v1/auth/setup-admin",
                                "/api/v1/auth/login",
                                "/api/v1/auth/csrf",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/actuator/health",
                                "/error"
                        ).permitAll()

                        // User Management (Admin only)
                        .requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")

                        // Product Management RBAC
                        .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PURCHASE_MANAGER", "SALES_STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/products/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/products", "/api/v1/products/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/products", "/api/v1/products/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")

                        // Category Management RBAC
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PURCHASE_MANAGER", "SALES_STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories", "/api/v1/categories/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories", "/api/v1/categories/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories", "/api/v1/categories/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER")

                        // Supplier Management RBAC
                        .requestMatchers(HttpMethod.GET, "/api/v1/suppliers", "/api/v1/suppliers/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PURCHASE_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/suppliers", "/api/v1/suppliers/**").hasAnyRole("ADMIN", "PURCHASE_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/suppliers", "/api/v1/suppliers/**").hasAnyRole("ADMIN", "PURCHASE_MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/suppliers", "/api/v1/suppliers/**").hasAnyRole("ADMIN", "PURCHASE_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/suppliers", "/api/v1/suppliers/**").hasAnyRole("ADMIN", "PURCHASE_MANAGER")

                        // Purchase Management RBAC
                        .requestMatchers(HttpMethod.POST, "/api/v1/purchases/*/receive").hasAnyRole("ADMIN", "PURCHASE_MANAGER", "INVENTORY_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/purchases", "/api/v1/purchases/**").hasAnyRole("ADMIN", "PURCHASE_MANAGER", "INVENTORY_MANAGER")
                        .requestMatchers("/api/v1/purchases", "/api/v1/purchases/**").hasAnyRole("ADMIN", "PURCHASE_MANAGER")

                        // Sales Management RBAC
                        .requestMatchers(HttpMethod.GET, "/api/v1/sales", "/api/v1/sales/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PURCHASE_MANAGER", "SALES_STAFF")
                        .requestMatchers("/api/v1/sales", "/api/v1/sales/**").hasAnyRole("ADMIN", "SALES_STAFF")

                        // Smart Inventory Insights RBAC
                        .requestMatchers("/api/v1/insights/stock-valuation", "/api/v1/insights/reorder-recommendations").hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PURCHASE_MANAGER")
                        .requestMatchers("/api/v1/insights", "/api/v1/insights/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PURCHASE_MANAGER", "SALES_STAFF")

                        // Low-Stock Alerts RBAC
                        .requestMatchers("/api/v1/alerts", "/api/v1/alerts/**").hasAnyRole("ADMIN", "INVENTORY_MANAGER", "PURCHASE_MANAGER", "SALES_STAFF")

                        // All other authenticated endpoints (including /api/v1/auth/me and /api/v1/auth/logout)
                        .anyRequest().authenticated()
                )
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
