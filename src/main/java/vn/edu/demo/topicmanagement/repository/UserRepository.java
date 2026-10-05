package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.entity.UserRole;
import vn.edu.demo.topicmanagement.entity.UserStatus;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    // Các hàm "existsBy..." trả về true/false: dùng để kiểm tra trùng.
    // Hậu tố "AndIdNot" nghĩa là "trừ chính người có id này" (dùng khi SỬA tài khoản).
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByStudentCode(String studentCode);
    boolean existsByStudentCodeAndIdNot(String studentCode, Long id);
    boolean existsByLecturerCode(String lecturerCode);
    boolean existsByLecturerCodeAndIdNot(String lecturerCode, Long id);

    // "left join fetch": lấy kèm bộ môn; left join vì Admin có thể không thuộc bộ môn nào
    @Query("select u from User u left join fetch u.department order by u.id")
    List<User> findAllWithDepartment();

    // Danh sách giảng viên theo vai trò + trạng thái (dùng cho ô chọn GVHD thứ hai)
    List<User> findByRoleAndStatusOrderByFullNameAsc(UserRole role, UserStatus status);
}
