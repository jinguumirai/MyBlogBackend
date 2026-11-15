package net.myBlog.common.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Spring Securityの{@link SecurityContextHolder}から現在のユーザーIDを取得する実装。
 *
 * <p>JWT認証フィルタ(Loginサービスの{@code JwtAuthenticationFilter}と同じ方式)が
 * トークンのsubject(ユーザーID)をprincipalへ設定している前提で、
 * principalを数値のIDへ変換する。</p>
 *
 * <p>未認証({@code authentication}がnull)、またはprincipalをIDとして解釈できない場合は
 * 匿名ユーザーIDを返す。DBの{@code create_user_id}などの監査列がNOT NULLでも
 * 登録できるようにするためのフォールバックである。</p>
 */
public class SecurityContextCurrentUserIdProvider implements CurrentUserIdProvider {

    /** ユーザーを特定できない場合に使用するID(既定null) */
    private final Integer anonymousUserId;

    public SecurityContextCurrentUserIdProvider(Integer anonymousUserId) {
        this.anonymousUserId = anonymousUserId;
    }

    @Override
    public Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return anonymousUserId;
        }
        return toUserId(authentication.getPrincipal());
    }

    /**
     * principalをユーザーIDへ変換する。
     *
     * <p>JWTのsubjectは文字列のため、数値へ変換できない場合は匿名ユーザーIDを返す。</p>
     */
    private Integer toUserId(Object principal) {
        if (principal instanceof Number number) {
            return number.intValue();
        }
        if (principal == null) {
            return anonymousUserId;
        }
        try {
            return Integer.valueOf(principal.toString().trim());
        } catch (NumberFormatException e) {
            return anonymousUserId;
        }
    }
}
