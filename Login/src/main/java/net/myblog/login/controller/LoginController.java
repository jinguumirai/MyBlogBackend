package net.myblog.login.controller;

import jakarta.validation.Valid;
import net.myblog.login.controller.dto.EmailLoginDto;
import net.myblog.login.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {
    @Autowired
    private LoginService loginService;

    @PostMapping("/email")
    public boolean loginByEmail(@RequestBody @Valid EmailLoginDto dto) {
        // Here you would typically validate the email and password against your user database
        // For simplicity, we will assume the credentials are valid and return a token
        // return loginService.getToken(email);
        return loginService.loginByEmail(dto.getEmail(), dto.getPassword());
    }

    @PostMapping("/validate")
    public String validateToken(String token) {
        if (loginService.validateToken(token)) {
            return "Token is valid.";
        } else {
            return "Token is invalid.";
        }
    }

    @GetMapping("/test")
    public String getTest()
    {
        return "Hello, this is a test endpoint.";
    }
}
