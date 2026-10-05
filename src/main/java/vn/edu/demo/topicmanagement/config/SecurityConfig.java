package vn.edu.demo.topicmanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Cấu hình phân quyền: ai được vào trang nào. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Ai cũng vào được: trang đăng nhập và file tĩnh
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                // Phân quyền theo vai trò (đường dẫn bắt đầu bằng ... thì chỉ vai trò đó được vào)
                .requestMatchers("/admin/**").hasRole("ADMIN")                  // Quản trị hệ thống
                .requestMatchers("/periods/**", "/head/**").hasRole("HEAD")     // Trưởng khoa
                .requestMatchers("/lecturer/**").hasRole("LECTURER")            // Giảng viên
                .requestMatchers("/student/**").hasRole("STUDENT")              // Sinh viên

                // Còn lại: chỉ cần đăng nhập
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }
}
