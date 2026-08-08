package net.myblog.login.service;
import net.myblog.login.entity.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.blog.common.validationIdentity.JwtUtil;

@Service
public class LoginService {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    public String getToken(String email) {
        return jwtUtil.generateToken(email);
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    public boolean loginByEmail(String email, String password)
    {
        var userList = userRepository.findByEmail(email);
        if (userList.isEmpty())
        {
            return false;
        }
        else
        {
            var user = userList.get();
            return user.getPassword().equals(password) && user.isEnabled();
        }
    }
}
