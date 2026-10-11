# Bổ sung chức năng Trưởng khoa phân công/thay đổi GVHD

## Phạm vi chỉnh sửa

- Thêm GET/POST `/head/topics/{id}/supervisors` trong `HeadTopicController.java`.
- Thêm trang `src/main/resources/templates/head/topics/supervisors.html` và liên kết ở danh sách đề tài Trưởng khoa.
- Thêm `HeadSupervisorForm.java`, `HeadSupervisorInfo.java`, phương thức `TopicService.updateSupervisorsByHead(...)`.
- Bổ sung truy vấn `TopicRegistrationRepository.findByTopic_Id(...)`.
- Thêm CSS cho liên kết thao tác.

**Không cần chạy thêm SQL:** dùng bảng `topic_supervisors` đã có trong database.

## Quy tắc nghiệp vụ

- Chỉ HEAD có quyền vào `/head/**` (theo `SecurityConfig.java`).
- GVHD chính bắt buộc; GVHD thứ hai không bắt buộc; tối đa 2 giảng viên, không trùng nhau.
- Chỉ giảng viên có trạng thái `ACTIVE` mới được chọn.
- Không đổi GVHD khi đề tài có kết quả hội đồng, hoặc chọn người đã làm phản biện / thành viên hội đồng của chính đề tài đó.
- Không phân công GVHD cho đề tài đang `REJECTED`.
- Mọi thay đổi danh sách GVHD nằm trong một transaction; không thay tên người tạo đề tài (`created_by`).

## Hướng dẫn kiểm thử trên localhost

1. Sao lưu `E:\lam2\topicmanagement` và database trên cổng 3307.
2. Dừng Spring Boot đang chạy (Ctrl+C trong terminal VS Code).
3. Giải nén gói sửa đổi, cập nhật thư mục `topicmanagement` (không xóa thư mục `uploads` đang có).
4. Từ thư mục có `pom.xml`, chạy ` .\mvnw.cmd spring-boot:run ` bằng JDK 25.
5. Đăng nhập `head01` -> `/head/topics` -> **Phân công / đổi GVHD** ở đề tài **Xây dựng hệ thống quản lý thư viện** (đang chưa có kết quả). **Không thử đổi GVHD ở đề tài KLTN Nhóm 1** vì đề tài này đã công bố điểm.
6. Chọn giảng viên chính + tùy chọn giảng viên thứ hai -> Lưu, quay về danh sách xác minh tên GVHD.
7. Thử chọn trùng GVHD chính/thứ hai để xác minh hệ thống báo lỗi.
8. Thử đổi GVHD ở một đề tài đã có kết quả, hệ thống phải từ chối; dữ liệu cũ được giữ nguyên.
9. Đăng nhập với vai trò không phải HEAD và thử URL `/head/topics`: phải bị chặn.

Đã bổ sung kiểm tra: nếu Trưởng khoa đổi GVHD chính khác với giảng viên tạo đề tài, người tạo không thể sửa/xóa đề tài đang chờ duyệt để tự đặt lại GVHD chính. Nếu GVHD chính vẫn là người tạo, biểu mẫu giảng viên vẫn có quyền điều chỉnh GVHD thứ hai khi đề tài đang chờ duyệt / bị từ chối.
