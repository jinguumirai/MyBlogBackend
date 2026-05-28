package net.myblog.login.controller;

import jakarta.validation.Valid;
import net.myblog.login.controller.dto.LoginDto;
import net.myblog.login.entity.User;
import net.myblog.login.entity.UserRepository;
import net.myblog.login.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/register")
public class RegistrationController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public String login(@RequestBody @Valid LoginDto loginDto)
    {
        var userName = loginDto.getUserName();
        var password = loginDto.getPassword();
        return "Hello " + userName + ", your password is " + password;
    }

    @PostMapping
    public String register(String email, String password)
    {
        if (userRepository.findByEmail(email).isPresent())
        {
            return "User has been registered.";
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
        return "Certification mail has been sent to your email address.";
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String token)
    {
        var userOpt = userRepository.findByVerificationToken(token);
        if (userOpt.isEmpty())
        {
            return "No effective token";
        }

        User user = userOpt.get();
        user.setEnabled(true);
        userRepository.save(user);

        return "Registration has been completed.";
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
