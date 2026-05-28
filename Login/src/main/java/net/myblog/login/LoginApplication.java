package net.myblog.login;

import net.blog.common.validationIdentity.JwtUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(JwtUtil.class)
public class LoginApplication {
    public static void main(String[] args)
    {
        SpringApplication.run(LoginApplication.class, args);
    }
}
