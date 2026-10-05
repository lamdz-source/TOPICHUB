package vn.edu.demo.topicmanagement.entity;

/** Trạng thái tài khoản - khớp với cột users.status. */
public enum UserStatus {
    ACTIVE("Hoạt động"),
    INACTIVE("Đã khóa");

    private final String label;
    UserStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
