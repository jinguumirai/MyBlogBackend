package net.myblog.login.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import net.myBlog.common.BasicEntity;

@Entity
@Table(name="user_info")
public class User extends BasicEntity {

    private String email;
    @Column(name="user_password")
    private String password;


    private Boolean verified = false;

    @Column(name="user_name")
    private String userName;

    /** メール認証用の6桁の数字コード。register時に照合し、登録完了後は破棄する。 */
    @Column(name="verification_code")
    private String verificationCode;

    public User()
    {
        ;
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

    public Boolean isVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }
}
