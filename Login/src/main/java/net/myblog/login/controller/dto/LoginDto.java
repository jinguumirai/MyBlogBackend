package net.myblog.login.controller.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginDto {
    @NotBlank(message = "Username cannot be blank")
    public String userName;
    @NotBlank(message = "Password cannot be blank")
    public String password;

    public LoginDto(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
