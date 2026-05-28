package net.myblog.login.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz ->
                authz.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/identify/login/**", "/doc.html", "/webjars/&zwnj;**", "/v2/api-docs",
                        "/swagger-resources/**&zwnj;", "identify/api-docs/**", "identify/v3/api-docs/**",
                        "/login/**", "/v3/api-docs/**", "/login").permitAll().anyRequest().authenticated());
        return httpSecurity.build();
    }
}
