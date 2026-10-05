package vn.edu.demo.topicmanagement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.UserForm;
import vn.edu.demo.topicmanagement.dto.PasswordChangeForm;
import vn.edu.demo.topicmanagement.dto.AccountProfileView;
import vn.edu.demo.topicmanagement.entity.Department;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.entity.UserStatus;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/** Nghiệp vụ quản lý tài khoản (chức năng của Admin). */
@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository userRepository,
                              DepartmentRepository departmentRepository,
                              PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllWithDepartment();
    }

    @Transactional(readOnly = true)
    public List<Department> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    /** Returns only non-secret details for the authenticated account. */
    @Transactional(readOnly = true)
    public AccountProfileView getOwnProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        return new AccountProfileView(user.getUsername(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getStatus(), user.getStudentCode(), user.getLecturerCode(),
                user.getDepartment() == null ? null : user.getDepartment().getName(), user.getCreatedAt());
    }

    /** Checks the current password, validates confirmation, then stores the new BCrypt hash. */
    @Transactional
    public void changeOwnPassword(String username, PasswordChangeForm form) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác.");
        }
        if (form.getNewPassword() == null || form.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới nhập lại không khớp.");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
    }

    /**
     * Kiểm tra quy tắc nghiệp vụ. existingId = null nghĩa là đang TẠO MỚI, khác null là đang SỬA.
     * Trả về Map: tên ô nhập -> câu báo lỗi (rỗng = hợp lệ).
     */
    public Map<String, String> checkRules(UserForm f, Long existingId) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean creating = (existingId == null);

        // Khi SỬA thì vai trò lấy từ database (không cho đổi vai trò); khi TẠO thì lấy từ form
        UserRole role;
        if (creating) {
            role = f.getRole();
        } else {
            // Read the value explicitly instead of Optional.map so Eclipse does not
            // report an unchecked null-annotation conversion for Function<User, UserRole>.
            var existingUser = userRepository.findById(existingId);
            role = existingUser.isPresent() ? existingUser.get().getRole() : null;
        }

        // --- Mật khẩu ---
        String pw = f.getPassword();
        if (creating && (isBlank(pw) || pw.length() < 6)) {
            errors.put("password", "Mật khẩu tối thiểu 6 ký tự");
        } else if (!creating && !isBlank(pw) && pw.length() < 6) {
            errors.put("password", "Mật khẩu mới tối thiểu 6 ký tự (hoặc để trống nếu không đổi)");
        }

        // --- Tên đăng nhập không được trùng (chỉ kiểm tra khi tạo; khi sửa không cho đổi) ---
        if (creating && f.getUsername() != null && userRepository.existsByUsername(f.getUsername().trim())) {
            errors.put("username", "Tên đăng nhập đã tồn tại");
        }

        // --- Email không được trùng ---
        String email = blankToNull(f.getEmail());
        if (email != null) {
            boolean duplicated = creating
                    ? userRepository.existsByEmail(email)
                    : userRepository.existsByEmailAndIdNot(email, existingId);
            if (duplicated) errors.put("email", "Email đã được dùng cho tài khoản khác");
        }

        // --- Mã SV / mã GV phụ thuộc vai trò (khớp với CHECK trong database) ---
        String sc = blankToNull(f.getStudentCode());
        String lc = blankToNull(f.getLecturerCode());
        if (role == UserRole.STUDENT) {
            if (sc == null) {
                errors.put("studentCode", "Sinh viên bắt buộc có mã sinh viên");
            } else if (creating ? userRepository.existsByStudentCode(sc)
                                : userRepository.existsByStudentCodeAndIdNot(sc, existingId)) {
                errors.put("studentCode", "Mã sinh viên đã tồn tại");
            }
            if (lc != null) errors.put("lecturerCode", "Sinh viên không có mã giảng viên");
        } else if (role == UserRole.HEAD || role == UserRole.LECTURER) {
            if (lc == null) {
                errors.put("lecturerCode", "Giảng viên / Trưởng khoa bắt buộc có mã giảng viên");
            } else if (creating ? userRepository.existsByLecturerCode(lc)
                                : userRepository.existsByLecturerCodeAndIdNot(lc, existingId)) {
                errors.put("lecturerCode", "Mã giảng viên đã tồn tại");
            }
            if (sc != null) errors.put("studentCode", "Giảng viên / Trưởng khoa không có mã sinh viên");
        } else if (role == UserRole.ADMIN) {
            if (sc != null) errors.put("studentCode", "Admin không có mã sinh viên");
            if (lc != null) errors.put("lecturerCode", "Admin không có mã giảng viên");
        }

        // --- Bộ môn: bắt buộc với mọi vai trò trừ Admin ---
        if (role != null && role != UserRole.ADMIN && f.getDepartmentId() == null) {
            errors.put("departmentId", "Vui lòng chọn bộ môn");
        }
        if (f.getDepartmentId() != null && !departmentRepository.existsById(f.getDepartmentId())) {
            errors.put("departmentId", "Bộ môn không tồn tại");
        }
        return errors;
    }

    @Transactional
    public void create(UserForm f) {
        User u = new User();
        u.setUsername(f.getUsername().trim());
        u.setPassword(passwordEncoder.encode(f.getPassword()));   // mã hóa BCrypt trước khi lưu
        u.setRole(f.getRole());
        u.setStatus(UserStatus.ACTIVE);
        fill(u, f, f.getRole());
        userRepository.save(u);
    }

    /** Dùng khi mở trang SỬA: đọc dữ liệu từ DB ra form (không đưa mật khẩu ra). */
    @Transactional(readOnly = true)
    public UserForm getFormForEdit(Long id) {
        User u = getById(id);
        UserForm f = new UserForm();
        f.setUsername(u.getUsername());
        f.setFullName(u.getFullName());
        f.setEmail(u.getEmail());
        f.setPhone(u.getPhone());
        f.setRole(u.getRole());
        f.setStudentCode(u.getStudentCode());
        f.setLecturerCode(u.getLecturerCode());
        f.setDepartmentId(u.getDepartment() == null ? null : u.getDepartment().getId());
        return f;
    }

    @Transactional
    public void update(Long id, UserForm f) {
        User u = getById(id);
        // Vai trò và tên đăng nhập KHÔNG được đổi: dù form có bị sửa trộm cũng bỏ qua
        fill(u, f, u.getRole());
        if (!isBlank(f.getPassword())) {
            u.setPassword(passwordEncoder.encode(f.getPassword()));
        }
        userRepository.save(u);
    }

    /** Khóa / mở khóa tài khoản. Không cho tự khóa chính mình (tránh mất quyền Admin). */
    @Transactional
    public void toggleStatus(Long id, String currentUsername) {
        User u = getById(id);
        if (u.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("Không thể khóa chính tài khoản đang đăng nhập.");
        }
        u.setStatus(u.getStatus() == UserStatus.ACTIVE ? UserStatus.INACTIVE : UserStatus.ACTIVE);
        userRepository.save(u);
    }

    /** Xóa vĩnh viễn tài khoản nếu không còn dữ liệu nghiệp vụ tham chiếu đến tài khoản đó. */
    @Transactional
    public void delete(Long id, String currentUsername) {
        User user = getById(id);
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("Không thể xóa tài khoản đang đăng nhập.");
        }
        userRepository.delete(user);
        userRepository.flush(); // chạy DELETE ngay để controller bắt được lỗi khóa ngoại
    }

    // ------------------------------------------------------------------
    private User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"));
    }

    /** Chép dữ liệu form vào entity; mã SV/GV chỉ giữ lại nếu đúng với vai trò. */
    private void fill(User u, UserForm f, UserRole role) {
        u.setFullName(f.getFullName().trim());
        u.setEmail(blankToNull(f.getEmail()));
        u.setPhone(blankToNull(f.getPhone()));
        u.setStudentCode(role == UserRole.STUDENT ? blankToNull(f.getStudentCode()) : null);
        u.setLecturerCode((role == UserRole.HEAD || role == UserRole.LECTURER)
                ? blankToNull(f.getLecturerCode()) : null);
        u.setDepartment(f.getDepartmentId() == null
                ? null : departmentRepository.getReferenceById(f.getDepartmentId()));
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    // Chuỗi rỗng -> null, vì cột email/mã trong DB có UNIQUE: nhiều chuỗi "" sẽ bị coi là trùng nhau
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
