package net.myBlog.common.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 監査項目(登録者・登録日時・更新者・更新日時)の自動補完に関する設定。
 *
 * <ul>
 *     <li>{@code common.audit.enabled}: 自動補完の有効/無効。未設定の場合は有効</li>
 *     <li>{@code common.audit.anonymous-user-id}: 未ログインなどでユーザーを特定できない場合に
 *         監査列へ設定するID。既定は0(システムユーザー扱い)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "common.audit")
public class AuditProperties {

    /** 監査項目の自動補完を有効にするか */
    private boolean enabled = true;

    /** ユーザーを特定できない場合に使用するID */
    private Integer anonymousUserId = 0;

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getAnonymousUserId() {
        return anonymousUserId;
    }

    public void setAnonymousUserId(Integer anonymousUserId) {
        this.anonymousUserId = anonymousUserId;
    }
}
