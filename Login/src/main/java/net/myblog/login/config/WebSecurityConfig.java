package net.myblog.login.config;

import net.myblog.login.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class WebSecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public WebSecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                // Authorizationヘッダ(Bearer)/Cookie(token)のJWTを検証して認証情報を設定する(認証不要パスには影響しない)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authz ->
                authz.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/identify/login/**", "/doc.html", "/webjars/&zwnj;**", "/v2/api-docs",
                        "/swagger-resources/**&zwnj;", "identify/api-docs/**", "identify/v3/api-docs/**",
                        "/login/**", "/v3/api-docs/**", "/login",
                        // 登録(send-code/register)は未ログインユーザーが呼ぶため認証不要にする
                        "/register/**").permitAll().anyRequest().authenticated());
        return httpSecurity.build();
    }
}
