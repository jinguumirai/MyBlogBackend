package net.myblog.login.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.myBlog.common.jwt.JwtTokenVerifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Authorizationヘッダ(Bearer)またはHttpOnly Cookie(token)から取得したJWTを読み取り、
 * 検証してSecurityContextへ認証情報を設定するフィルタ。
 *
 * <p>ログインAPIがSet-Cookieしたトークンを以降のリクエストで自動的に検証し、
 * さらにAPIクライアントが送る {@code Authorization: Bearer <token>} のJWTも検証する。
 * どちらの形式でも WebSecurityConfig の anyRequest().authenticated() で保護されたAPI
 * (例: /test/hello) へ認証済みアクセスを可能にする。</p>
 *
 * <p>トークンの検証には全マイクロサービス共通の{@link JwtTokenVerifier}(CommonLib)を使用する。
 * 他のサービスでも同じ検証器を使うことで、サービス間で検証ロジックがずれない。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** ログイン成功時にブラウザへ保存するトークンCookie名(LoginControllerと同名) */
    private static final String TOKEN_COOKIE_NAME = "token";

    /** AuthorizationヘッダのBearerスキーム接頭辞 */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenVerifier jwtTokenVerifier;

    public JwtAuthenticationFilter(JwtTokenVerifier jwtTokenVerifier) {
        this.jwtTokenVerifier = jwtTokenVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 既に認証済みでなければ、Authorizationヘッダ(Bearer)またはCookie(token)のJWTを検証して認証情報を設定する
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateWithValidToken(request);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorizationヘッダ(Bearer)を優先し、続いてCookie(token)から取得したJWTを
     * 有効なものが見つかるまで検証し、見つかればSecurityContextへ認証情報を設定する。
     */
    private void authenticateWithValidToken(HttpServletRequest request) {
        for (String token : resolveTokenCandidates(request)) {
            if (jwtTokenVerifier.validateToken(token)) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        jwtTokenVerifier.extractSubject(token), null, List.of());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                return;
            }
        }
    }

    /** Authorizationヘッダ(Bearer) → Cookie(token) の優先順でトークン候補を返す。 */
    private List<String> resolveTokenCandidates(HttpServletRequest request) {
        List<String> candidates = new ArrayList<>();
        String headerToken = readTokenFromAuthorizationHeader(request);
        if (headerToken != null) {
            candidates.add(headerToken);
        }
        String cookieToken = readTokenFromCookie(request);
        if (cookieToken != null) {
            candidates.add(cookieToken);
        }
        return candidates;
    }

    /**
     * リクエストの {@code Authorization: Bearer <token>} ヘッダからトークンを取り出す。
     * ヘッダが無い・Bearer形式でない・値が空の場合はnull。
     */
    private String readTokenFromAuthorizationHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    /** リクエストのCookieから token の値を取り出す。無ければnull。 */
    private String readTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
