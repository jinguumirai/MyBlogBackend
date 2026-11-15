package net.myBlog.common.jwt;

import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * PEM形式の鍵ファイルを読み込むユーティリティ。全マイクロサービス共通で使用する。
 *
 * <p>各サービスは検証用の公開鍵のみを保持し、署名用の秘密鍵は認証サービス(Login)だけが保持する。</p>
 */
public final class PemKeyLoader {

    private PemKeyLoader() {
    }

    /**
     * PEM形式のEC公開鍵(X.509 SubjectPublicKeyInfo)を読み込み、{@link PublicKey}を返す。
     *
     * @param resource PEMファイルの場所(classpath:/file:など)
     * @return EC公開鍵
     * @throws IllegalStateException 読み込みに失敗した場合
     */
    public static PublicKey loadEcPublicKey(Resource resource) {
        try {
            byte[] der = readPem(resource, "PUBLIC KEY");
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("JWT公開鍵の読み込みに失敗しました: " + resource, e);
        }
    }

    /**
     * PEMファイルからヘッダー/フッターを除いたDERバイト列を取り出す。
     *
     * @param resource PEMファイルの場所
     * @param type     ヘッダーに書かれた鍵種別(例: "PUBLIC KEY", "EC PRIVATE KEY")
     * @return DERバイト列
     * @throws IOException PEMの形式が不正な場合
     */
    public static byte[] readPem(Resource resource, String type) throws IOException {
        String pem;
        try (InputStream in = resource.getInputStream()) {
            pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String header = "-----BEGIN " + type + "-----";
        String footer = "-----END " + type + "-----";
        int start = pem.indexOf(header);
        int end = pem.indexOf(footer);
        if (start < 0 || end < 0 || end <= start) {
            throw new IOException("PEMの形式が不正です。期待する型: " + type);
        }
        String base64 = pem.substring(start + header.length(), end).replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
