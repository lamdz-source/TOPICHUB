package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Ứng với bảng "users". */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String password; // lưu mã hóa BCrypt, không lưu mật khẩu thô

    @Column(name = "full_name")
    private String fullName;

    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "student_code")   // chỉ có với sinh viên
    private String studentCode;

    @Column(name = "lecturer_code")  // chỉ có với giảng viên / trưởng khoa
    private String lecturerCode;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Để MySQL tự điền giờ tạo (DEFAULT CURRENT_TIMESTAMP): Java không gửi giá trị này xuống
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public String getStudentCode() { return studentCode; }
    public String getLecturerCode() { return lecturerCode; }
    public UserStatus getStatus() { return status; }
    public Department getDepartment() { return department; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(UserRole role) { this.role = role; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public void setLecturerCode(String lecturerCode) { this.lecturerCode = lecturerCode; }
    public void setStatus(UserStatus status) { this.status = status; }
    public void setDepartment(Department department) { this.department = department; }
}
