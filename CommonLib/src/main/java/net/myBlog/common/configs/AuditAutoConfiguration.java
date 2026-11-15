package net.myBlog.common.configs;

import net.myBlog.common.audit.AuditProperties;
import net.myBlog.common.audit.AuditUserIdHolder;
import net.myBlog.common.audit.CurrentUserIdProvider;
import net.myBlog.common.audit.SecurityContextCurrentUserIdProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 監査項目(登録者・登録日時・更新者・更新日時)の自動補完を構成する。
 *
 * <p>本クラスは「現在の操作ユーザーIDの供給元」を共通で用意するだけで、
 * 実際の補完はフレームワーク別の構成が担う。</p>
 *
 * <ul>
 *     <li>MyBatis-Plus: {@link MybatisAuditAutoConfiguration}({@code MetaObjectHandler})</li>
 *     <li>JPA/Hibernate: {@code BasicEntity}の{@code @PrePersist}/{@code @PreUpdate}
 *         (MyBatisもMyBatis-Plusも不要)</li>
 * </ul>
 *
 * <p>既定の供給元は{@link SecurityContextCurrentUserIdProvider}で、
 * Spring Securityの{@code SecurityContextHolder}からログイン中のユーザーID
 * (JWTのsubject)を取り出す。変更したい場合は{@link CurrentUserIdProvider}の実装を
 * Bean定義すれば既定の実装は無効になる。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(
        prefix = "common.audit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true   // 未設定の場合は有効とみなす
)
public class AuditAutoConfiguration {

    /** ログイン中のユーザーIDをSecurityContextから取得する既定実装を登録する。 */
    @Bean
    @ConditionalOnMissingBean(CurrentUserIdProvider.class)
    public CurrentUserIdProvider currentUserIdProvider(AuditProperties auditProperties) {
        return new SecurityContextCurrentUserIdProvider(auditProperties.getAnonymousUserId());
    }

    /**
     * JPAのエンティティコールバック({@code BasicEntity})へ供給元を引き渡す。
     *
     * <p>コールバックはSpring管理外のエンティティから呼び出されるため、
     * Beanを{@link AuditUserIdHolder}経由で共有する。</p>
     */
    @Bean
    public AuditUserIdHolderConfigurer auditUserIdHolderConfigurer(CurrentUserIdProvider currentUserIdProvider) {
        return new AuditUserIdHolderConfigurer(currentUserIdProvider);
    }

    /** {@link AuditUserIdHolder}へ供給元を設定するだけの構成用Bean。 */
    public static final class AuditUserIdHolderConfigurer {

        private AuditUserIdHolderConfigurer(CurrentUserIdProvider currentUserIdProvider) {
            AuditUserIdHolder.setProvider(currentUserIdProvider);
        }
    }
}
