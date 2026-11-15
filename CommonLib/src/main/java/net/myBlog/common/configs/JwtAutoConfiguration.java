package net.myBlog.common.configs;

import io.jsonwebtoken.Jwts;
import net.myBlog.common.jwt.JwtTokenVerifier;
import net.myBlog.common.jwt.PemKeyLoader;
import net.myBlog.common.properties.JwtProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 全マイクロサービス共通のJWT検証機能を自動構成する。
 *
 * <p>{@code jwt.public-key-path}を設定したサービスのみ{@link JwtTokenVerifier}が
 * Beanとして登録されるため、JWTを使わないサービスへは影響しない。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
@ConditionalOnClass(Jwts.class)
@ConditionalOnProperty(
        prefix = "jwt",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true   // 未設定の場合は有効とみなす
)
public class JwtAutoConfiguration {

    /**
     * 設定された公開鍵を読み込んでJWT検証器を生成する。
     *
     * <p>鍵の読み込みは起動時に行うため、パスや形式の設定ミスは起動時に検知できる。</p>
     */
    @Bean
    @ConditionalOnMissingBean(JwtTokenVerifier.class)
    @ConditionalOnProperty(prefix = "jwt", name = "public-key-path")
    public JwtTokenVerifier jwtTokenVerifier(JwtProperties jwtProperties) {
        return new JwtTokenVerifier(PemKeyLoader.loadEcPublicKey(jwtProperties.getPublicKeyPath()));
    }
}
