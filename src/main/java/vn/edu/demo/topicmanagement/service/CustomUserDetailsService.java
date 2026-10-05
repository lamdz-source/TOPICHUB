package vn.edu.demo.topicmanagement.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.entity.UserStatus;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Khi người dùng bấm "Đăng nhập", Spring Security gọi class này để tìm user trong database,
 * rồi tự so sánh mật khẩu đã nhập với mật khẩu BCrypt trong DB.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        // Dùng tên đầy đủ của lớp User của Spring để không nhầm với entity User của mình
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())                       // ADMIN -> quyền ROLE_ADMIN
                .disabled(user.getStatus() == UserStatus.INACTIVE)  // tài khoản bị khóa thì không đăng nhập được
                .build();
    }
}
