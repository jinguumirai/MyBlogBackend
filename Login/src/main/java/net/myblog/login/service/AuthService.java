package net.myblog.login.service;

import io.jsonwebtoken.Jwts;
import net.myBlog.common.jwt.PemKeyLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * JWTトークンの生成を担う認証サービス。
 *
 * <p>署名には{@code jwt.private-key-path}で指定したEC秘密鍵(P-256)を使用する。
 * 署名アルゴリズムはES256。秘密鍵はこのサービスだけが保持する。</p>
 *
 * <p>トークンの検証は全マイクロサービス共通の{@link net.myBlog.common.jwt.JwtTokenVerifier}
 * (CommonLib)が担当する。</p>
 */
@Service
public class AuthService {

    /** トークン有効期限の既定値(ミリ秒)。1時間。 */
    private static final long DEFAULT_EXPIRATION_MS = 3600000L;

    /** id-ecPublicKey (1.2.840.10045.2.1) のDER値 */
    private static final byte[] OID_ID_EC_PUBLIC_KEY = {
            (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0xce, (byte) 0x3d, 0x02, 0x01
    };

    /** prime256v1 / P-256 (1.2.840.10045.3.1.7) のDER値 */
    private static final byte[] OID_PRIME_256_V1 = {
            (byte) 0x2a, (byte) 0x86, (byte) 0x48, (byte) 0xce, (byte) 0x3d, 0x03, 0x01, 0x07
    };

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
                .signWith(getPrivateKey(), Jwts.SIG.ES256)
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
     * PEM形式のEC秘密鍵を読み込む。
     * <p>PEMのEC秘密鍵は通常SEC1形式(RFC 5915)のため、
     * Java標準のKeyFactoryで読めるPKCS#8形式(RFC 5208)に変換してから読み込む。</p>
     */
    private PrivateKey loadPrivateKey() {
        try {
            byte[] sec1 = PemKeyLoader.readPem(privateKeyResource, "EC PRIVATE KEY");
            byte[] pkcs8 = convertSec1ToPkcs8(sec1);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT秘密鍵の読み込みに失敗しました: " + privateKeyResource, e);
        }
    }

    /**
     * SEC1形式(RFC 5915)のEC秘密鍵をPKCS#8形式(RFC 5208)へ変換する。
     *
     * <p>PKCS#8のAlgorithmIdentifierに曲線のOIDを含めるため、SEC1内の
     * 曲線パラメータ([0])は除去して再エンコードする(OpenSSLの正規形式と同じ)。</p>
     */
    private byte[] convertSec1ToPkcs8(byte[] sec1) throws IOException {
        DerReader reader = new DerReader(sec1);
        DerValue outer = reader.readValue();
        if (outer.tag != 0x30) {
            throw new IOException("SEC1秘密鍵の先頭がSEQUENCEではありません。");
        }

        DerReader inner = new DerReader(outer.content);
        DerValue version = inner.readValue();
        if (version.tag != 0x02) {
            throw new IOException("SEC1秘密鍵のversionがINTEGERではありません。");
        }
        DerValue privateKey = inner.readValue();
        if (privateKey.tag != 0x04) {
            throw new IOException("SEC1秘密鍵のprivateKeyがOCTET STRINGではありません。");
        }

        // 残りのフィールド([0]曲線パラメータ,[1]公開鍵など)を走査する
        byte[] curveOid = null;
        List<byte[]> trailing = new ArrayList<>();
        while (inner.hasRemaining()) {
            DerValue value = inner.readValue();
            if (value.tag == 0xA0) {
                // [0] ECParameters は曲線を特定するためだけに使い、PKCS#8には含めない
                if (curveOid == null) {
                    curveOid = extractCurveOid(value.content);
                }
                continue;
            }
            trailing.add(value.encode());
        }
        if (curveOid == null) {
            // パラメータが無い場合はP-256とみなす
            curveOid = OID_PRIME_256_V1;
        }

        // 曲線パラメータを除去したSEC1を再エンコード
        ByteArrayOutputStream sec1Content = new ByteArrayOutputStream();
        sec1Content.write(version.encode());
        sec1Content.write(privateKey.encode());
        for (byte[] field : trailing) {
            sec1Content.write(field);
        }
        byte[] sec1Reencoded = encodeTlv(0x30, sec1Content.toByteArray());

        // AlgorithmIdentifier: SEQUENCE { OID(id-ecPublicKey), OID(曲線) }
        byte[] algId = buildAlgorithmIdentifier(curveOid);

        // PKCS#8: SEQUENCE { INTEGER(0), AlgorithmIdentifier, OCTET STRING(SEC1) }
        byte[] octetString = encodeTlv(0x04, sec1Reencoded);
        ByteArrayOutputStream pkcs8 = new ByteArrayOutputStream();
        pkcs8.write(0x30);
        writeDerLength(pkcs8, 3 + algId.length + octetString.length);
        pkcs8.write(0x02);
        pkcs8.write(0x01);
        pkcs8.write(0x00); // version = 0
        pkcs8.write(algId);
        pkcs8.write(octetString);
        return pkcs8.toByteArray();
    }

    /** ECParameters([0]の内容)からnamed curve OIDを取り出す。 */
    private byte[] extractCurveOid(byte[] parameters) throws IOException {
        DerReader reader = new DerReader(parameters);
        if (!reader.hasRemaining()) {
            return null;
        }
        DerValue first = reader.readValue();
        if (first.tag == 0x06) {
            return first.content;
        }
        if (first.tag == 0x30) {
            DerReader inner = new DerReader(first.content);
            if (inner.hasRemaining()) {
                DerValue oid = inner.readValue();
                if (oid.tag == 0x06) {
                    return oid.content;
                }
            }
        }
        return null;
    }

    /**
     * EC用のAlgorithmIdentifierを組み立てる。
     * SEQUENCE { OID(1.2.840.10045.2.1 id-ecPublicKey), OID(曲線) }
     */
    private byte[] buildAlgorithmIdentifier(byte[] curveOid) throws IOException {
        ByteArrayOutputStream algId = new ByteArrayOutputStream();
        algId.write(encodeTlv(0x06, OID_ID_EC_PUBLIC_KEY));
        algId.write(encodeTlv(0x06, curveOid));
        return encodeTlv(0x30, algId.toByteArray());
    }

    /** TLV(tag + length + content)を組み立てる。 */
    private static byte[] encodeTlv(int tag, byte[] content) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        writeDerLength(out, content.length);
        out.write(content);
        return out.toByteArray();
    }

    /** DERのLengthフィールドを書き出す。 */
    private static void writeDerLength(ByteArrayOutputStream out, int length) {
        if (length < 0x80) {
            out.write(length);
        } else if (length <= 0xFF) {
            out.write(0x81);
            out.write(length);
        } else if (length <= 0xFFFF) {
            out.write(0x82);
            out.write((length >>> 8) & 0xFF);
            out.write(length & 0xFF);
        } else {
            out.write(0x83);
            out.write((length >>> 16) & 0xFF);
            out.write((length >>> 8) & 0xFF);
            out.write(length & 0xFF);
        }
    }

    /** 最小限のDER(TLV)読み取り値。 */
    private static final class DerValue {
        private final int tag;
        private final byte[] content;

        private DerValue(int tag, byte[] content) {
            this.tag = tag;
            this.content = content;
        }

        /** tag + length + content として再エンコードする。 */
        private byte[] encode() throws IOException {
            return encodeTlv(tag, content);
        }
    }

    /** 最小限のDER読み取りストリーム。 */
    private static final class DerReader {
        private final byte[] data;
        private int pos;

        private DerReader(byte[] data) {
            this.data = data;
            this.pos = 0;
        }

        private boolean hasRemaining() {
            return pos < data.length;
        }

        private DerValue readValue() throws IOException {
            if (pos >= data.length) {
                throw new IOException("DER: データが途中で終わっています。");
            }
            int tag = data[pos++] & 0xFF;
            int length = readLength();
            if (pos + length > data.length) {
                throw new IOException("DER: 長さがデータを超えています。");
            }
            byte[] content = Arrays.copyOfRange(data, pos, pos + length);
            pos += length;
            return new DerValue(tag, content);
        }

        private int readLength() throws IOException {
            if (pos >= data.length) {
                throw new IOException("DER: データが途中で終わっています。");
            }
            int first = data[pos++] & 0xFF;
            if ((first & 0x80) == 0) {
                return first;
            }
            int count = first & 0x7F;
            if (count == 0 || count > 4) {
                throw new IOException("DER: 長さフィールドが不正です。");
            }
            int length = 0;
            for (int i = 0; i < count; i++) {
                if (pos >= data.length) {
                    throw new IOException("DER: データが途中で終わっています。");
                }
                length = (length << 8) | (data[pos++] & 0xFF);
            }
            return length;
        }
    }
}


