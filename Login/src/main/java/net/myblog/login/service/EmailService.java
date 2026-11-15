package net.myblog.login.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 登録用の6桁の数字コードをメールで送信する。
     *
     * @param to   送信先メールアドレス
     * @param code メール本文に記載する6桁の数字コード
     */
    public void sendVerificationCodeEmail(String to, String code) {
        String subject = "Mail address certification";
        String text = "Your certification code is: " + code + "\n"
                + "Please enter this 6-digit code in the registration form to complete your registration.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        message.setFrom(from);
        mailSender.send(message);
    }
}
