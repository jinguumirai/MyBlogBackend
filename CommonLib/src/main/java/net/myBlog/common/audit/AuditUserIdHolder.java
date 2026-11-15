package net.myBlog.common.audit;

/**
 * エンティティのコールバック(JPA)から現在の操作ユーザーIDを参照するための静的ホルダ。
 *
 * <p>JPAの{@code @PrePersist}/{@code @PreUpdate}はSpring管理外のエンティティから
 * 呼び出されるため、Beanを静的フィールドで共有する
 * (Spring Data JPAの{@code AuditingEntityListener}と同じ方式)。
 * 供給元は{@code net.myBlog.common.configs.AuditAutoConfiguration}が起動時に設定する。</p>
 *
 * <p>設定されていない場合(Spring管理外での利用や単体テストなど)は、
 * SecurityContextから取得しユーザーを特定できない場合は0を返す既定実装を使用する。</p>
 */
public final class AuditUserIdHolder {

    /** 既定の供給元(Spring管理外で使用された場合のフォールバック) */
    private static final CurrentUserIdProvider DEFAULT_PROVIDER = new SecurityContextCurrentUserIdProvider(0);

    /** 現在の供給元 */
    private static volatile CurrentUserIdProvider provider = DEFAULT_PROVIDER;

    private AuditUserIdHolder() {
    }

    /**
     * 供給元を設定する(起動時に{@code AuditAutoConfiguration}が呼び出す)。
     *
     * @param currentUserIdProvider 供給元。nullの場合は既定実装へ戻す
     */
    public static void setProvider(CurrentUserIdProvider currentUserIdProvider) {
        provider = currentUserIdProvider == null ? DEFAULT_PROVIDER : currentUserIdProvider;
    }

    /**
     * 現在の操作ユーザーIDを返す。
     *
     * @return 操作ユーザーID(ユーザーを特定できない場合は匿名ユーザーID)
     */
    public static Integer currentUserId() {
        return provider.getCurrentUserId();
    }
}
