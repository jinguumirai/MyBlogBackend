package net.myblog.login.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 本登録リクエスト。
 * フロントエンドは { "email": "...", "password": "...", "verificationCode": "123456" } をJSONで送信する。
 */
public class RegisterDto {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email format is invalid")
    private String email;
    @NotBlank(message = "Password cannot be blank")
    private String password;
    /** メールで送信された6桁の数字コード */
    @NotBlank(message = "Verification code cannot be blank")
    @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
    private String verificationCode;

    public RegisterDto(String email, String password, String verificationCode) {
        this.email = email;
        this.password = password;
        this.verificationCode = verificationCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}
