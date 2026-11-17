package net.myblog.login.service;

import io.jsonwebtoken.Jwts;
import net.myBlog.common.jwt.PemKeyLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.UUID;

/**
 * JWTトークンの生成を担う認証サービス。
 *
 * <p>署名には{@code jwt.private-key-path}で指定したRSA秘密鍵(PKCS#8形式のPEM)を使用する。
 * 署名アルゴリズムはRS256(RSA 2048bit以上)。秘密鍵はこのサービスだけが保持する。</p>
 *
 * <p>トークンの検証は全マイクロサービス共通の{@link net.myBlog.common.jwt.JwtTokenVerifier}
 * (CommonLib)が担当する。</p>
 */
@Service
public class AuthService {

    /** トークン有効期限の既定値(ミリ秒)。1時間。 */
    private static final long DEFAULT_EXPIRATION_MS = 3600000L;

    @Value("${jwt.private-key-path}")
    private Resource privateKeyResource;

    /** 生成時に使用する秘密鍵(初回アクセス時に遅延ロード) */
    private PrivateKey cachedPrivateKey;

    /**
     * 指定されたsubject(通常はemailまたはユーザー名)に対するJWTトークンを生成する。
     *
     * @param subject トークンの主体
     * @return 署名済みJWTトークン
     */
    public String generateToken(String subject) {
        Date now = new Date();
        /*　トークン有効期限(ミリ秒)。既定は1時間。 */
        return Jwts.builder()
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + DEFAULT_EXPIRATION_MS))
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * トークンの既定の有効期限(秒)を返す。
     * CookieのMax-Ageをトークンの有効期限と揃えるために使用する。
     *
     * @return 有効期限(秒)
     */
    public long getDefaultExpirationSeconds()
    {
        return DEFAULT_EXPIRATION_MS / 1000;
    }

    /** 秘密鍵を遅延ロードする(スレッドセーフ)。 */
    private synchronized PrivateKey getPrivateKey() {
        if (cachedPrivateKey == null) {
            cachedPrivateKey = loadPrivateKey();
        }
        return cachedPrivateKey;
    }

    /**
     * PEM形式のRSA秘密鍵(PKCS#8)を読み込む。
     *
     * <p>{@code openssl genpkey -algorithm RSA}で生成した
     * 「{@code -----BEGIN PRIVATE KEY-----}」形式のPKCS#8 PEMをそのまま読み込める。</p>
     */
    private PrivateKey loadPrivateKey() {
        try {
            byte[] pkcs8 = PemKeyLoader.readPem(privateKeyResource, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT秘密鍵の読み込みに失敗しました: " + privateKeyResource, e);
        }
    }
}

