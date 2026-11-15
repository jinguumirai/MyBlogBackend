package net.myBlog.common.configs;

import net.myBlog.common.audit.AuditInterceptor;
import net.myBlog.common.audit.CurrentUserIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis(およびMyBatis-Plus)を使うサービス向けの監査項目自動補完の構成。
 *
 * <p>MyBatis本体が依存に含まれる場合のみ有効になる({@link ConditionalOnClass})。
 * MyBatisのインターセプタをBean登録するだけで、MyBatis-Plusの有無にかかわらず
 * INSERT/UPDATE時に監査項目が補完される
 * (MyBatis-Plusも{@link Interceptor}のBeanを自動的に取り込む)。</p>
 *
 * <p>補完するユーザーIDの供給元は{@code AuditAutoConfiguration}が登録する
 * {@link CurrentUserIdProvider}を使用する(MyBatisを使わないJPAサービスと共通)。</p>
 */
@AutoConfiguration
@ConditionalOnClass(Interceptor.class)
@ConditionalOnProperty(
        prefix = "common.audit",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true   // 未設定の場合は有効とみなす
)
public class MybatisAuditAutoConfiguration {

    /** 監査項目を補完するMyBatisインターセプタを登録する。 */
    @Bean
    @ConditionalOnMissingBean(AuditInterceptor.class)
    public AuditInterceptor auditInterceptor(CurrentUserIdProvider currentUserIdProvider) {
        return new AuditInterceptor(currentUserIdProvider);
    }
}
