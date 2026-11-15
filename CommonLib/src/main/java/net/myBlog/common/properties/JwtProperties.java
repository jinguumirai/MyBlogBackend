package net.myBlog.common.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * 全マイクロサービス共通のJWT検証設定。
 *
 * <p>トークン検証を利用するサービスは以下のプロパティを設定する。</p>
 * <ul>
 *     <li>{@code jwt.public-key-path}: 検証用公開鍵(PEM形式)の場所。設定されたサービスのみ検証器が生成される</li>
 *     <li>{@code jwt.enabled}: 検証機能の有効/無効。未設定の場合はtrue</li>
 * </ul>
 *
 * <p>既存の認証サービス(Login)の設定と互換性を保つため、プレフィックスは{@code jwt}とする。</p>
 */
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT検証機能を有効にするか */
    private boolean enabled = true;

    /** 検証に使用する公開鍵(PEM)の場所 */
    private Resource publicKeyPath;

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Resource getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(Resource publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }
}
