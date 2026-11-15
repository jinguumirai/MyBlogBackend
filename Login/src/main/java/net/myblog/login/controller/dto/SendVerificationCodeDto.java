package net.myblog.login.controller.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 登録用の認証コード送信リクエスト。
 * フロントエンドは { "email": "..." } をJSONで送信する。
 */
public class SendVerificationCodeDto {
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email format is invalid")
    private String email;

    public SendVerificationCodeDto(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
