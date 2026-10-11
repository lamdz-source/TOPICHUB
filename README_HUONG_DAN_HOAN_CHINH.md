# TOPICHUB – HỆ THỐNG QUẢN LÝ ĐỀ TÀI SINH VIÊN

**Môn học:** Lập trình Web – Đồ án cuối kỳ  
**Stack:** Java 25, Spring Boot 4.1.1, Spring MVC, Spring Security, Spring Data JPA, Thymeleaf, MySQL/MariaDB.

## Chạy trên máy Windows đã cài đặt

1. Sao lưu thư mục `E:\lam2\topicmanagement` và sao lưu database hiện tại nếu đã có dữ liệu.
2. Giải nén mã nguồn vào `E:\lam2\topicmanagement` (tránh tạo nhầm `topicmanagement\topicmanagement`).
3. Bật MariaDB XAMPP trên cổng 3307 và bảo đảm database `student_topic_management` đã tồn tại.
4. Cấu hình `src/main/resources/application.properties` (mặc định dùng `127.0.0.1:3307`, root, mật khẩu rỗng nếu XAMPP chưa đặt mật khẩu). Nếu có mật khẩu, đặt biến môi trường `DB_PASSWORD`.
5. Mở PowerShell:

```powershell
cd E:\lam2\topicmanagement
java -version
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

6. Vào `http://localhost:8080/login`.

> **Không chạy lại SQL khởi tạo trên database đang dùng**, vì script xóa rồi tạo lại database. Nếu chưa có database và thực sự muốn tạo mới, nhập file `database/student_topic_management_init_day_du.sql` trên đúng kết nối 3307.

## Quy trình demo

| Vai trò | Chức năng chính |
| --- | --- |
| Admin | Tài khoản, phân quyền, thông báo |
| Trưởng khoa | Đợt đăng ký, phê duyệt/công bố, GVHD, GVPB, hội đồng, kết quả |
| Giảng viên | Đề xuất đề tài, báo cáo của nhóm hướng dẫn, điểm phản biện, điểm hội đồng |
| Sinh viên | Tạo/tham gia nhóm, đăng ký đề tài, nộp PDF (nhóm trưởng), xem điểm |

**Dữ liệu mẫu:** Có một số tài khoản minh họa trong file SQL; mật khẩu mẫu chỉ dành cho localhost. Đổi mật khẩu trước khi trình diễn trên mạng công cộng.

## Tệp nộp bài

- `Bao_cao_do_an_cuoi_ky.docx`: báo cáo 6 chương và phụ lục.
- `Bao_cao_ket_qua_du_an.docx`: tóm tắt kết quả thực hiện, kiểm thử, hạn chế.
- `topicmanagement/`: mã nguồn và dữ liệu schema.
- `So_do/`: sơ đồ Use Case, ERD và mã nguồn sơ đồ chỉnh sửa được.
- `Checklist_kiem_thu.md`: các bước xác nhận sau khi giải nén.

**Tên ZIP nộp theo mẫu giảng viên:** `GroupXX_MSSV1_MSSV2_MSSV3_MSSV4_MSSV5.zip` (thay bằng thông tin nhóm thật). Không nên đổi tên ZIP trước khi có MSSV.
