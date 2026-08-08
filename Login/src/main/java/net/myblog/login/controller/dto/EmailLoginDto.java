package net.myblog.login.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class EmailLoginDto {
    @NotBlank(message = "Email cannot be blank")
    private String email;
    @NotBlank(message = "password cannot be blank")
    private String password;

    public EmailLoginDto(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
