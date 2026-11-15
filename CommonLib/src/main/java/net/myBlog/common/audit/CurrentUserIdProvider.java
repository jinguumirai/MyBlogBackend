package net.myBlog.common.audit;

/**
 * 監査項目(登録者・更新者)へ設定する「現在の操作ユーザーID」を提供する。
 *
 * <p>実装の既定は{@link SecurityContextCurrentUserIdProvider}(Spring Securityの
 * {@code SecurityContextHolder}から取得)であり、
 * {@code net.myBlog.common.configs.AuditAutoConfiguration}がBean登録する。
 * 取得方法をサービス固有にしたい場合は本インタフェースの実装をBean定義すれば
 * 既定の実装は無効になる({@code @ConditionalOnMissingBean})。</p>
 */
public interface CurrentUserIdProvider {

    /**
     * 現在の操作ユーザーIDを返す。
     *
     * <p>未ログインなどでユーザーが特定できない場合は、実装が定める
     * 匿名ユーザーID(既定は0)を返す。nullを返さないこと。</p>
     *
     * @return 操作ユーザーID
     */
    Integer getCurrentUserId();
}
