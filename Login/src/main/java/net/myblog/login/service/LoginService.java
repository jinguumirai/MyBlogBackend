package net.myblog.login.service;

import net.myBlog.common.exceptions.SystemException;
import net.myBlog.common.jwt.JwtTokenVerifier;
import net.myblog.login.entity.User;
import net.myblog.login.enums.ErrorEnums;
import net.myblog.login.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoginService {

    /** メールで送信する認証コードの桁数 */
    private static final int VERIFICATION_CODE_LENGTH = 6;

    public LoginService(AuthService authService, JwtTokenVerifier jwtTokenVerifier, UserRepository userRepository,
                        EmailService emailService)
    {
        this.authService = authService;
        this.jwtTokenVerifier = jwtTokenVerifier;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    private final AuthService authService;

    /** トークン検証は全マイクロサービス共通の実装(CommonLib)に委譲する */
    private final JwtTokenVerifier jwtTokenVerifier;

    private final UserRepository userRepository;

    /** 認証コードはメールで本人宛に送るため、ここで送信処理を行う */
    private final EmailService emailService;

    /**
     * 認証コード生成用の乱数。
     * 推測されると他人のメールアドレスで登録できてしまうため、予測可能な {@link java.util.Random} は使わない。
     * {@link SecureRandom} はスレッドセーフなので、インスタンスを共有してよい。
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * トークンが有効かどうかを検証する。
     *
     * @param token JWTトークン
     * @return 有効なトークンであればtrue
     */
    public boolean validateToken(String token) {
        return jwtTokenVerifier.validateToken(token);
    }

    /**
     * メールアドレスとパスワードでログイン認証を行う。
     * 認証に成功した場合はJWTトークンを発行して返す。
     * 発行したトークンはController側でブラウザのCookieに保存され、
     * 以後のリクエストではブラウザがそのCookieを自動送信する。
     *
     * @param email    ログインID(メールアドレス)
     * @param password パスワード
     * @return 発行されたJWTトークン
     * @throws SystemException ユーザーが存在しない/パスワード不一致/アカウントが無効の場合
     */
    public String loginByEmail(String email, String password)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SystemException(ErrorEnums.NOSUCHUSERORPASSWORDERROR.getCode(), null));

        // パスワード不一致・メール未認証のどちらでも同じエラーを返し、原因を外部から推測させない。
        // パスワードはDB上nullの可能性もあるため null安全に比較する。
        if (!Objects.equals(user.getPassword(), password) || !user.isVerified())
        {
            throw new SystemException(ErrorEnums.NOSUCHUSERORPASSWORDERROR.getCode(), null);
        }

        return authService.generateToken(user.getId().toString());
    }

    /**
     * 6桁の数字認証コードを生成し、指定されたメールアドレス宛にメールで送信する。
     *
     * <p>登録前のユーザーは未認証(disabled)の状態でuser_infoに作成し、生成したコードを保存する。
     * 同じメールアドレスで再送信された場合は、既存レコードのコードを新しいコードで上書きする
     * (コードは最新の1件のみ有効)。</p>
     *
     * <p>本登録前のユーザーは無効(enabled=false)のまま登録するため、パスワード照合ではログインできない。
     * user_name / user_password が NOT NULL のスキーマでも登録できるようにこの時点では仮値を保存し、
     * 本登録時にリクエストのパスワードで上書きする。</p>
     *
     * @param email 認証コードの送信先メールアドレス
     * @throws SystemException すでに登録済み(認証済み)のメールアドレスの場合
     */
    public void sendVerificationCode(String email)
    {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.isVerified())
        {
            throw new SystemException(ErrorEnums.EMAILALREADYREGISTERED.getCode(), null);
        }

        String verificationCode = generateVerificationCode();

        if (user == null)
        {
            user = new User();
            user.setEmail(email);
            // user_info の user_name / user_password は NOT NULL のため、本登録までの仮値を設定する。
            // 仮パスワードは推測不可能なランダム値にしておき、本登録時にリクエストのパスワードで上書きする
            // (そもそもアカウントは無効のため、仮パスワードではログインできない)。
            user.setUserName(email);
            user.setPassword(generatePlaceholderPassword());
        }
        user.setVerificationCode(verificationCode);
        userRepository.save(user);

        emailService.sendVerificationCodeEmail(email, verificationCode);
    }

    /**
     * メールで送信した6桁の数字認証コードを照合し、一致すれば本登録(アカウントの有効化)を行う。
     *
     * <p>照合に成功したコードは再利用できないように破棄する。
     * 認証コードを送信していない(未認証ユーザーが存在しない)、コードが一致しない、
     * すでに登録済みのいずれの場合も同じエラーを返し、原因を外部から推測させない。</p>
     *
     * @param email            登録するメールアドレス
     * @param password         登録するパスワード
     * @param verificationCode メールで受け取った6桁の数字コード
     * @return 登録完了メッセージ
     * @throws SystemException 認証コードが未送信/不一致/使用済み、または登録済みの場合
     */
    public String register(String email, String password, String verificationCode)
    {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new SystemException(ErrorEnums.INVALIDVERIFICATIONCODE.getCode(), null));

        if (user.isVerified())
        {
            throw new SystemException(ErrorEnums.EMAILALREADYREGISTERED.getCode(), null);
        }

        if (!Objects.equals(user.getVerificationCode(), verificationCode))
        {
            throw new SystemException(ErrorEnums.INVALIDVERIFICATIONCODE.getCode(), null);
        }

        user.setPassword(password);
        user.setVerified(true);
        // 使用済みのコードは再利用できないように破棄する
        user.setVerificationCode(null);
        userRepository.save(user);

        return "Registration has been completed.";
    }

    /**
     * 先頭が0の場合も6桁になるように、1桁ずつ乱数で組み立てた数字コードを生成する。
     *
     * @return 0埋めされた6桁の数字コード
     */
    private String generateVerificationCode()
    {
        StringBuilder code = new StringBuilder(VERIFICATION_CODE_LENGTH);
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++)
        {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    /**
     * 本登録前のユーザーに設定する仮パスワードを生成する。
     * user_password が NOT NULL のスキーマでも登録できるようにするための値であり、
     * 推測されても意味がないようランダムな文字列にする。
     *
     * @return 本登録時に上書きされる仮パスワード
     */
    private String generatePlaceholderPassword()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * 発行するトークンの有効期限(秒)を返す。
     * CookieのMax-Ageをトークンの有効期限と揃えるために使用する。
     *
     * @return トークンの有効期限(秒)
     */
    public long getTokenMaxAgeSeconds()
    {
        return authService.getDefaultExpirationSeconds();
    }
}
