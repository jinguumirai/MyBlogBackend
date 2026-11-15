package net.myBlog.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.PublicKey;
import java.util.Date;
import java.util.Objects;

/**
 * JWTトークンの検証を担うクラス。全マイクロサービス共通で使用する。
 *
 * <p>検証には公開鍵のみを使用するため、各サービスへ秘密鍵を配布する必要はない。
 * Bean化は{@code net.myBlog.common.configs.JwtAutoConfiguration}が行い、
 * {@code jwt.public-key-path}が設定されたサービスのみで利用可能になる。</p>
 *
 * <p>トークンの生成(署名)は認証サービス(Login)の責務であり、このクラスは行わない。</p>
 */
public class JwtTokenVerifier {

    /** 検証に使用する公開鍵 */
    private final PublicKey publicKey;

    public JwtTokenVerifier(PublicKey publicKey) {
        this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
    }

    /**
     * トークンの署名と有効期限を検証する。
     *
     * @param token JWTトークン
     * @return 有効なトークンであればtrue
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * トークンからsubjectを取り出す。
     *
     * @param token JWTトークン
     * @return subject
     */
    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * トークンからidを取り出す。
     *
     * <p>ログイン時にidをsubjectとして設定したトークンを発行しているため、
     * subjectをそのままidとして返す。トークンが不正な場合は例外をスローする。</p>
     *
     * @param token JWTトークン
     * @return id
     */
    public Integer extractId(String token) {
        return Integer.valueOf(extractSubject(token));
    }

    /**
     * トークンの有効期限を取り出す。
     *
     * @param token JWTトークン
     * @return 有効期限
     */
    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    /**
     * トークンを検証してClaimsを取得する。
     *
     * @param token JWTトークン
     * @return Claims
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
