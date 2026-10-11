# Rà soát đồ án cuối kỳ — lam2

**Nguồn rà soát:** `lam2.zip` và đề bài `Đồ án cuối kỳ.pdf` (3 trang). **Phạm vi:** rà soát cấu trúc, mã nguồn, SQL và tài liệu; chưa thực hiện kiểm thử khởi động với cơ sở dữ liệu MySQL thực tế.

## I. Tổng quan

- Dự án: `lam2/topicmanagement` — Java Spring Boot, Thymeleaf, Spring Security, Spring Data JPA, MySQL.
- 111 file Java trong `src/main/java`; 30 file giao diện HTML trong `src/main/resources/templates`.
- Có 16 bảng SQL, 1 VIEW và dữ liệu mẫu trong `lam2/student_topic_management_v2.sql`.
- Có tài liệu thiết kế dạng Markdown trong `docs/` và một bài kiểm thử `contextLoads()` tại `src/test/.../TopicmanagementApplicationTests.java`.
- `pom.xml` khai báo Spring Boot 4.1.1 và Java 25. Chưa kiểm thử build/chạy đầy đủ trong môi trường rà soát.

## II. Đối chiếu chức năng

| Đề bài | Mã nguồn | Nhận định |
|---|---|---|
| Tài khoản và phân quyền | `config/SecurityConfig.java`; `UserAccountService.java`; `AdminUserController.java` | Đã triển khai, cần kiểm thử các vai trò |
| Tạo/mở/đóng đợt có hai giai đoạn | `RegistrationPeriodService.java` | Đã triển khai tạo/sửa/xóa nháp, mở/đóng và kiểm tra thời gian |
| GV gửi đề tài, 1–2 GVHD | `TopicService.java`; `LecturerTopicController.java` | Có GVHD chính là người tạo; GVHD thứ hai được chọn |
| Trưởng khoa duyệt/công bố và phân công GVHD | `HeadTopicController.java` | Duyệt/công bố có; chưa thấy chức năng HEAD trực tiếp phân công hoặc thay đổi GVHD |
| Nhóm SV tối đa 3, một SV chỉ một nhóm | `StudentPortalService.java` | Có kiểm tra ở service và unique trong SQL; nên kiểm thử thao tác đồng thời |
| Nhóm trưởng đăng ký một đề tài | `StudentPortalService.java` | Đã có; cho phép đăng ký lại khi bị từ chối |
| Nhóm trưởng nộp báo cáo | `StudentReportService.java` | Đã có PDF/DOC/DOCX tối đa 10 MB; dữ liệu mẫu có đường dẫn tệp nhưng tệp không đi kèm ZIP |
| GVPB phản biện/chấm điểm | `ReviewerService.java`, `TopicReviewer.java` | Có logic; cần bổ sung cột SQL bằng migration |
| Hội đồng 3–5 GV, chủ tịch và thư ký | `CouncilService.java` | Đã có; hiện chỉ cho TLCN/KLTN, cần thống nhất phạm vi với giảng viên |
| Cấm GVHD chấm đề tài hướng dẫn | `ReviewerService.java`; `CouncilScoringService.java` | Có kiểm tra nghiệp vụ |
| Tổng hợp/công bố kết quả | `CouncilScoringService.java`, `HeadResultController.java` | Có triển khai; điểm cuối đang trung bình điểm thành viên hội đồng, chưa đưa điểm GVPB vào trung bình |
| Thông báo | `AnnouncementService.java` | Có tạo/công bố và lọc phạm vi |

## III. Việc cần làm theo thứ tự

### P0 — Làm hệ thống khởi động và chạy đúng
1. Kiểm tra `java -version`; dự án đòi Java 25 trong `pom.xml`; xác minh Maven wrapper, tải thư viện và chạy `./mvnw test` / `./mvnw spring-boot:run` trên máy có đủ công cụ.
2. Chuẩn bị MySQL và cấu hình `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (không đưa mật khẩu thật vào Git).
3. **CSDL mới:** dùng `student_topic_management_init_day_du.sql` được tạo kèm theo. **CSDL đã có dữ liệu:** chỉ áp dụng `database/migrations/002_add_reviewer_scores.sql` một lần sau khi backup; tuyệt đối không chạy SQL khởi tạo vì có `DROP DATABASE`.
4. Dùng dữ liệu với cửa sổ đăng ký còn hiệu lực cho ngày demo; đợt KLTN mẫu đã hết thời gian GV/SV đăng ký tính đến 10/10/2026.
5. Kiểm tra đường dẫn và sự tồn tại thật của các tệp báo cáo mẫu.

### P1 — Bám sát đề bài
6. Bổ sung chức năng HEAD phân công hoặc điều chỉnh GVHD cho đề tài, đảm bảo 1–2 GVHD/đề tài và không làm hỏng các dữ liệu đang có.
7. Xác nhận công thức điểm cuối: chỉ điểm hội đồng hay cả điểm GVPB; triển khai nếu quy chế yêu cầu.
8. Xác nhận hội đồng chỉ lập cho TLCN/KLTN hay cần áp dụng thêm cho môn học/NCKH.
9. Kiểm tra điều kiện phân quyền tại service, đặc biệt tải báo cáo và điểm số.

### P2 — Kiểm thử, giao diện và báo cáo
10. Thêm kiểm thử tự động và checklist thủ công cho 4 vai trò. Có ít nhất một luồng demo hoàn chỉnh từ khởi tạo đợt đến công bố kết quả.
11. Làm layout chung, điều hướng rõ theo role; giao diện thông báo lỗi/validation thống nhất, responsive.
12. Hoàn thiện tài liệu Word 6 chương theo PDF: giới thiệu + phân công; Spring MVC và ít nhất 3 kỹ thuật/module; Use Case + FR/NFR; ERD + các bảng; ảnh màn hình; kết luận; phụ lục cấu hình.
13. Chuẩn bị toàn bộ mã nguồn, script SQL, file Word và ảnh vào ZIP đúng tên yêu cầu `GroupXX_MSSV1_...zip`. Hosting và PowerPoint không bắt buộc.

## IV. Checklist kiểm tra trên máy

- [ ] Kết nối MySQL và chạy app không lỗi khởi động.
- [ ] ADMIN tạo/sửa/khóa user; user INACTIVE không đăng nhập được.
- [ ] HEAD mở đợt và đặt đúng hai giai đoạn.
- [ ] GV gửi đề tài, HEAD duyệt/công bố.
- [ ] SV tạo nhóm 1–3 thành viên, chỉ nhóm trưởng đăng ký.
- [ ] HEAD duyệt đăng ký, nhóm trưởng nộp file thật.
- [ ] HEAD phân GVPB khác GVHD; GVPB nộp điểm.
- [ ] HEAD lập hội đồng đủ 3–5 GV; GVHD không chấm đề tài được hướng dẫn.
- [ ] Hội đồng chấm, chủ tịch tổng hợp, HEAD công bố, SV xem kết quả.
- [ ] Quyền truy cập trực tiếp URL sai vai trò trả 403.
- [ ] Báo cáo Word đủ nội dung, ảnh minh họa chụp từ bản chạy thực tế.

## V. Tài liệu liên quan

- `lam2/topicmanagement/docs/ERD_VA_TU_DIEN_DU_LIEU.md`
- `lam2/topicmanagement/docs/KE_HOACH_DO_AN.md`
- `lam2/topicmanagement/HUONG_DAN_CODE_VA_FLOW.md`
- `lam2/topicmanagement/database/migrations/002_add_reviewer_scores.sql`
- `lam2/student_topic_management_v2.sql`

**Lưu ý:** các mục “đã triển khai” là đánh giá ở mức xem mã nguồn, chưa phải xác nhận hoạt động end-to-end.
