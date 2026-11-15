package net.myblog.login.controller;

import jakarta.validation.Valid;
import net.myBlog.common.responses.ApiResponse;
import net.myblog.login.controller.dto.LoginDto;
import net.myblog.login.controller.dto.RegisterDto;
import net.myblog.login.controller.dto.SendVerificationCodeDto;
import net.myblog.login.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
public class RegistrationController {
    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public String login(@RequestBody @Valid LoginDto loginDto)
    {
        var userName = loginDto.getUserName();
        var password = loginDto.getPassword();
        return "Hello " + userName + ", your password is " + password;
    }

    /**
     * 登録用の6桁の数字認証コードを生成し、リクエストのメールアドレス宛に送信する。
     * フロントエンドは { "email": "..." } をJSONで送信する。
     *
     * @param dto 送信先メールアドレス
     * @return 送信結果(ApiResponse形式)
     */
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<String>> sendVerificationCode(@RequestBody @Valid SendVerificationCodeDto dto)
    {
        loginService.sendVerificationCode(dto.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Certification code has been sent to your email address."));
    }

    /**
     * メールで受け取った6桁の数字コードを照合して本登録する。
     * フロントエンドは { "email": "...", "password": "...", "verificationCode": "123456" } をJSONで送信する。
     *
     * @param dto 登録内容(メールアドレス・パスワード・認証コード)
     * @return 登録結果(ApiResponse形式)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegisterDto dto)
    {
        String message = loginService.register(dto.getEmail(), dto.getPassword(), dto.getVerificationCode());
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @GetMapping("/test")
    public String getTest()
    {
        return "Hello Get";
    }

    @PostMapping("/post-test")
    public String postTest()
    {
        return "Hello Post";
    }
}
