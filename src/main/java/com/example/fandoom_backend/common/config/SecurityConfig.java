package com.example.fandoom_backend.common.config;

import com.example.fandoom_backend.community.CommunityRateLimitFilter;
import com.example.fandoom_backend.security.AuthRateLimitFilter;
import com.example.fandoom_backend.security.JwtAuthenticationFilter;
import com.example.fandoom_backend.security.RestAccessDeniedHandler;
import com.example.fandoom_backend.security.RestAuthenticationEntryPoint;
import com.example.fandoom_backend.user.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final CommunityRateLimitFilter communityRateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable) // JWT Authorization header'da taşınıyor, cookie kullanılmıyor
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // JwtAuthenticationFilter'dan SONRA: userId bazlı limit, SecurityContext'te
                // authentication zaten çözülmüş olmalı (bkz. CommunityRateLimitFilter javadoc).
                .addFilterAfter(communityRateLimitFilter, JwtAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/resend-verification").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/franchises/**", "/api/genres/**", "/api/movies/**",
                                "/api/series/**", "/api/seasons/**", "/api/episodes/**",
                                "/api/people/**", "/api/characters/**", "/api/cast/**",
                                "/api/productions/**", "/api/cms/**", "/api/blogs/**", "/api/tags/**",
                                "/api/lore/**", "/api/likes/*/*/count", "/api/follows/*/*/count",
                                "/api/bookmarks/*/*/count", "/api/users/*/lists/pinned",
                                "/api/community/threads/**", "/api/community/feed/**").permitAll()
                        .requestMatchers("/api/cms/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/franchises/**", "/api/genres/**", "/api/movies/**",
                                "/api/series/**", "/api/seasons/**", "/api/episodes/**",
                                "/api/people/**", "/api/characters/**", "/api/cast/**",
                                "/api/media/**", "/api/blogs/**", "/api/tags/**",
                                "/api/lore/**").hasAnyRole("EDITOR", "MODERATOR", "ADMIN")
                        // Community yazma/etkileşim uçları: rol şartı yok (herhangi bir
                        // USER), sadece login yeterli. Sahip/moderatör ayrımı path
                        // seviyesinde ifade edilemediği için (aynı path hem sahibinin hem
                        // moderatörün isteğini kabul eder) serviste kontrol edilir
                        // (bkz. ThreadServiceImpl/CommentServiceImpl). Explicit listelendi
                        // ki "yeni yazma ucu eklenip path'e eklenmeyi unutma" riskinin
                        // sessizce anyRequest().authenticated()'a düşmesi belgelenmiş olsun.
                        .requestMatchers(HttpMethod.POST, "/api/community/threads",
                                "/api/community/threads/*/comments", "/api/community/threads/*/like",
                                "/api/community/threads/*/bookmark", "/api/community/comments/*/like")
                                .authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/community/threads/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/community/threads/**",
                                "/api/community/comments/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }
}
