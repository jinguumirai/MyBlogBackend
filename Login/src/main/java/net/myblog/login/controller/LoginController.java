package net.myblog.login.controller;

import jakarta.validation.Valid;
import net.myBlog.common.exceptions.SystemException;
import net.myBlog.common.responses.ApiResponse;
import net.myblog.login.controller.dto.EmailLoginDto;
import net.myblog.login.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/login")
public class LoginController {

    /** ログイン成功後にブラウザへ保存するトークンCookie名 */
    private static final String TOKEN_COOKIE_NAME = "token";

    @Autowired
    private LoginService loginService;

    /**
     * CookieにSecure属性を付与するかどうか。
     * HTTPS(本番)ではtrue、HTTP(開発)ではfalseをapplication-{profile}.ymlの
     * jwt.cookie-secureで設定する。
     */
    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/email")
    public ResponseEntity<ApiResponse<String>> loginByEmail(@RequestBody @Valid EmailLoginDto dto) {
        // 認証成功時のみJWTトークンが返る(失敗時はSystemExceptionがthrowされる)
        String token = loginService.loginByEmail(dto.getEmail(), dto.getPassword());

        // 発行したtokenをブラウザのCookieに保存する。
        // 以降、ブラウザはリクエスト毎にこのCookie(token)を自動送信するため、
        // 各サービス側はCookieからトークンを取得して認証を行える。
        ResponseCookie tokenCookie = ResponseCookie.from(TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofSeconds(loginService.getTokenMaxAgeSeconds()))
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookie.toString())
                .body(ApiResponse.success(token));
    }

    @GetMapping("/test")
    public String getTest()
    {
        return "Hello, this is a test endpoint.";
    }

    @GetMapping("/test-response")
    public ResponseEntity<ApiResponse<String>> getTestResponse()
    {
        throw new SystemException("500", "系统异常");
        // return ResponseEntity.ok(ApiResponse.success("Hello, this is a test endpoint."));
    }

    @GetMapping("/generate-token")
    public ResponseEntity<ApiResponse<String>> generateToken(@RequestParam("email") String email,
                                                             @RequestParam("password") String password)
    {
        return ResponseEntity.ok(ApiResponse.success(loginService.loginByEmail(email, password)));
    }
}
