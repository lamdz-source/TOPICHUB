# HOÀN THIỆN ĐỒ ÁN – BẢN TỔNG HỢP

## Nguồn và phạm vi

Được bổ sung trên nền mã nguồn `lam2_GVHD_full.zip`, đối chiếu đề bài `Đồ án cuối kỳ.pdf` và các luồng đã kiểm tra thủ công trên localhost. Giữ nguyên schema SQL hiện tại, không viết lại database.

## Thay đổi

1. **Trưởng khoa – GVHD:** đã có nút Phân công/đổi GVHD từ bản cập nhật trước; bảo toàn 1–2 GV và tránh trùng GV chấm.
2. **Trưởng khoa – đợt đăng ký:** thêm nút `Mở lại` chỉ cho đợt CLOSED còn hạn sinh viên đăng ký, lịch hợp lệ và chưa gắn hội đồng. Nghiệp vụ được kiểm tra trong service, không chỉ tại UI.
3. **Giao diện chung:** thêm `templates/fragments/nav.html`, navbar có điều kiện phân quyền theo vai trò, trang chủ dashboard, CSS responsive, viewport meta cho tất cả giao diện.
4. **Kiểm thử:** thêm `RegistrationPeriodServiceTest` với bốn trường hợp cho mở lại đợt.
5. **Tài liệu:** sơ đồ Use Case và ERD, báo cáo Word, báo cáo kết quả dự án, hướng dẫn sử dụng, checklist nghiệp vụ.

## Cần xác minh trên máy người dùng

- Build `mvnw.cmd clean test` với JDK 25, Maven có mạng để tải các thư viện lần đầu.
- Build và khởi động `mvnw.cmd spring-boot:run` trên MariaDB cổng 3307.
- Giao diện hiển thị ở màn hình laptop/mobile thực tế.
- Thao tác mở lại một đợt CLOSED đang còn hạn; đợt KLTN đã có hội đồng phải từ chối.
- Phân công GVHD với đề tài đã có kết quả hội đồng phải từ chối.

## An toàn dữ liệu

- **Không chạy lại** `student_topic_management_init_day_du.sql` trên database đang có dữ liệu: script có `DROP DATABASE`.
- Thư mục `uploads/reports` được đóng gói như bản người dùng gửi; khi cập nhật lên máy hãy giữ bản lưu hiện tại trước khi chép.
- Không dùng mật khẩu mẫu trên hosting công khai; kết nối database nên được cấu hình qua biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
