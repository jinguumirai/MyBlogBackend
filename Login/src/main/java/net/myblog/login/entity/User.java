package net.myblog.login.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name="user_info")
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String email;
    @Column(name="user_password")
    private String password;
    private boolean enabled = false;

    @Column(name="user_name")
    private String userName;

    @Column(name="verification_token")
    private String verificationToken = UUID.randomUUID().toString();

    public User(Long id, String email, String password, boolean enabled, String verificationToken,
                String userName) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.verificationToken = verificationToken;
        this.userName = userName;
    }

    public User()
    {
        ;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }
}
