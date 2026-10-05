package vn.edu.demo.topicmanagement.entity;

/**
 * Trạng thái đề tài - khớp với cột topics.status.
 * Luồng: PENDING (chờ duyệt) -> APPROVED (đã duyệt) -> PUBLISHED (đã công bố cho sinh viên)
 *        hoặc PENDING -> REJECTED (bị từ chối, giảng viên sửa lại sẽ về PENDING).
 */
public enum TopicStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Bị từ chối"),
    PUBLISHED("Đã công bố");

    private final String label;
    TopicStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
