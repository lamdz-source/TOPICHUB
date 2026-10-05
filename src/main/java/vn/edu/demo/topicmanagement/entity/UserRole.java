package vn.edu.demo.topicmanagement.entity;

/** Vai trò người dùng - khớp với cột users.role. Có kèm nhãn tiếng Việt để hiển thị. */
public enum UserRole {
    ADMIN("Quản trị hệ thống"),
    HEAD("Trưởng khoa"),
    LECTURER("Giảng viên"),
    STUDENT("Sinh viên");

    private final String label;
    UserRole(String label) { this.label = label; }
    public String getLabel() { return label; }
}
