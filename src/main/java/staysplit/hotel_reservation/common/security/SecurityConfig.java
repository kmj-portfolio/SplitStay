package staysplit.hotel_reservation.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import staysplit.hotel_reservation.common.security.jwt.CustomAccessDeniedHandler;
import staysplit.hotel_reservation.common.security.jwt.CustomAuthenticationEntryPoint;
import staysplit.hotel_reservation.common.security.jwt.JwtTokenFilter;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final JwtTokenFilter jwtTokenFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/customers/sign-up",
            "/api/providers/sign-up",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/reviews/**",
            "/api/likes/**",
            "/api/hotels/search"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/hotels/**",
            "/api/rooms/**",
            "/api/reviews/**",
            "/api/likes/**",
            "/api/photos/**"
    };

    private static final String[] OAUTH_ENDPOINTS  = {
            "/oauth2/authorization/google",
            "/login/oauth2/code/**",
            "/api/customers/google/login",
            "/api/customers/oauth/signup",
            "/oauth/**",
            "/api/oauth/**"
    };

    private static final String[] OAUTH_GET_ENDPOINTS  = {
            "/api/customers/google/callback",
            "/api/customers/kakao/callback",
    };

    private static final String[] CUSTOMER_GET_ENDPOINTS = {
            "/api/customers/**",
            "/api/reservations",
            "/api/reservations/**",
            "/api/likes/**",
            "/api/payments/**"
    };

    private static final String[] CUSTOMER_POST_ENDPOINTS = {
            "/api/customers/**",
            "/api/reservations",
            "/api/reservations/**",
            "/api/reviews/**",
            "/api/likes/**",
            "/api/payments/**",
    };

    private static final String[] CUSTOMER_PUT_ENDPOINTS = {
            "/api/customers/**",
            "/api/reviews/**",
            "/api/likes/**",
    };

    private static final String[] CUSTOMER_DELETE_ENDPOINTS = {
            "/api/customers/**",
            "/api/reviews/**",
            "/api/likes/**",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, CustomAccessDeniedHandler customAccessDeniedHandler) throws Exception {

        return httpSecurity
                .cors(cors -> cors.configurationSource(configurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        .requestMatchers("/v3/**", "/swagger-ui/**").permitAll()
                        .requestMatchers(OAUTH_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, OAUTH_GET_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/hotels/*", "/api/rooms/*", "/api/photos", "/api/photos/*").hasRole("PROVIDER")
                        .requestMatchers(HttpMethod.PUT, "/api/hotels/*", "/api/rooms/*").hasRole("PROVIDER")
                        .requestMatchers(HttpMethod.DELETE, "/api/hotels/*", "/api/rooms/*").hasRole("PROVIDER")
                        .requestMatchers(HttpMethod.GET, CUSTOMER_GET_ENDPOINTS).hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, CUSTOMER_PUT_ENDPOINTS).hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, CUSTOMER_POST_ENDPOINTS).hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.DELETE, CUSTOMER_DELETE_ENDPOINTS).hasRole("CUSTOMER")
                        .requestMatchers("/api/users/auth/status").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)

                // Customized spring security error handling
                .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint)
                                                                                .accessDeniedHandler(customAccessDeniedHandler))
                .build();
    }

    @Bean
    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173","http://localhost:3000", frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("*")); // 모든 http method 허용
        configuration.setAllowedHeaders(Arrays.asList("*")); // 모든 http header 허용
        configuration.setAllowCredentials(true); // Authorization header 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
