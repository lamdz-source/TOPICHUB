# Hướng dẫn đọc code dự án

Tài liệu này là bản đồ các tệp mã nguồn chính. Đọc từ trên xuống theo một luồng: Controller nhận URL/form → DTO nhận dữ liệu form → Service kiểm tra quy tắc → Repository truy vấn/lưu → Entity ánh xạ bảng MySQL → Thymeleaf hiển thị kết quả.

## 1. Khởi động và cấu hình

| Tệp | Vai trò |
|---|---|
| `TopicmanagementApplication.java` | Điểm bắt đầu Spring Boot; chạy `main()` để khởi động ứng dụng. |
| `pom.xml` | Danh sách thư viện Java (Spring MVC, JPA, Security, Thymeleaf, Validation, MySQL) và phiên bản Java. |
| `src/main/resources/application.properties` | Địa chỉ/kết nối MySQL, cấu hình JPA/Thymeleaf và giới hạn/thư mục upload báo cáo. Có thể truyền `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REPORT_UPLOAD_DIR` qua biến môi trường. |
| `config/SecurityConfig.java` | Quy định URL nào cần đăng nhập và role nào được truy cập; cấu hình trang login/logout và BCrypt. |
| `service/CustomUserDetailsService.java` | Đọc username/password/role/trạng thái từ DB để Spring Security xác thực. |

## 2. Controller: URL và điều hướng

| Tệp | Chức năng/URL chính |
|---|---|
| `HomeController.java` | Trang chủ `/` và đăng nhập `/login`. |
| `AccountController.java` | Người dùng đã đăng nhập đổi mật khẩu `/account/password`; xác nhận mật khẩu hiện tại trước khi lưu. |
| `AdminUserController.java` | Admin quản lý tài khoản `/admin/users`: xem, tạo, sửa, khóa/mở khóa, xóa nếu không còn khóa ngoại tham chiếu. |
| `AdminAnnouncementController.java` | Admin tạo/công bố thông báo trường `/admin/announcements`. |
| `HeadAnnouncementController.java` | Trưởng khoa tạo/công bố thông báo trường hoặc bộ môn `/head/announcements`. |
| `AnnouncementController.java` | Người dùng xem thông báo được phép thấy `/announcements`. |
| `RegistrationPeriodController.java` | Trưởng khoa tạo/mở/đóng đợt `/periods`. |
| `LecturerTopicController.java` | Giảng viên tạo/sửa/xem và xóa đề tài chưa có đăng ký sinh viên `/lecturer/topics`. |
| `HeadTopicController.java` | Trưởng khoa duyệt/từ chối/công bố đề tài `/head/topics`. |
| `TopicController.java` | Danh sách đề tài đã công bố `/topics`. |
| `StudentGroupController.java` | Sinh viên tạo/tham gia nhóm `/student/groups`. |
| `StudentRegistrationController.java` | Nhóm trưởng đăng ký đề tài và sinh viên xem trạng thái `/student/registrations`. |
| `HeadRegistrationController.java` | Trưởng khoa duyệt/từ chối đăng ký SV `/head/registrations`. |
| `StudentReportController.java` | Nhóm trưởng nộp, thành viên cùng nhóm tải báo cáo `/student/reports`. |
| `LecturerReportController.java` | GVPB/thành viên hội đồng tải báo cáo được giao `/lecturer/reports/{id}/download`. |
| `HeadReviewerController.java` | Trưởng khoa phân công GVPB `/head/reviewers`. |
| `LecturerReviewController.java` | Giảng viên gửi/cập nhật điểm phản biện `/lecturer/reviews`. |
| `HeadCouncilController.java` | Trưởng khoa tạo hội đồng, thêm thành viên/đề tài, kích hoạt `/head/councils`. |
| `HeadResultController.java` | HEAD xem các kết quả đã tổng hợp và công bố cho sinh viên `/head/results`. |
| `LecturerCouncilController.java` | Thành viên hội đồng chấm điểm; chủ tịch tổng hợp/công bố `/lecturer/council-work`. |
| `StudentResultController.java` | Sinh viên xem kết quả đã công bố của nhóm `/student/results`. |

## 3. Service: quy tắc nghiệp vụ

| Tệp | Vai trò |
|---|---|
| `UserAccountService.java` | Kiểm tra dữ liệu tài khoản, mã hóa BCrypt, sửa/khóa/xóa an toàn và đổi mật khẩu của chính mình. |
| `TopicService.java` | Tạo/sửa đề tài; chỉ người tạo được xóa đề tài PENDING/REJECTED chưa có đăng ký SV; kiểm tra đợt, bộ môn, GVHD; duyệt/từ chối/công bố. |
| `RegistrationPeriodService.java` | Kiểm tra hai giai đoạn và trường ngày theo loại; tạo/mở/đóng đợt. |
| `StudentPortalService.java` | Giới hạn thành viên nhóm, đăng ký đúng đề tài/đợt và duyệt đăng ký. |
| `StudentReportService.java` | Chỉ nhóm trưởng nộp báo cáo đã được duyệt; giới hạn loại/kích thước; chỉ thành viên nhóm tải. |
| `ReviewerService.java` | Phân công GVPB không trùng GVHD, chặn nộp điểm quá hạn. |
| `CouncilService.java` | Kiểm tra hội đồng có 3–5 GV, một chủ tịch, một thư ký; chỉ xếp đề tài có báo cáo và đủ điểm GVPB. |
| `CouncilScoringService.java` | Chấm điểm; GVHD được miễn chấm đề tài mình hướng dẫn; trung bình tính trên thành viên đủ điều kiện; chủ tịch tổng hợp, HEAD công bố. |
| `AnnouncementService.java` | Tạo/công bố thông báo và lọc phạm vi trường/bộ môn theo quyền người đọc. |

## 4. Entity: lớp ánh xạ bảng

Mỗi entity đại diện một bảng SQL; `@ManyToOne`/`@JoinColumn` nối khóa ngoại, `@Enumerated(EnumType.STRING)` lưu tên enum dạng chữ.

| Entity | Bảng | Entity | Bảng |
|---|---|---|---|
| `Department` | `departments` | `User` | `users` |
| `RegistrationPeriod` | `registration_periods` | `Topic` | `topics` |
| `TopicSupervisor` | `topic_supervisors` | `StudentGroup` | `student_groups` |
| `GroupMember` | `group_members` | `TopicRegistration` | `topic_registrations` |
| `TopicReviewer` | `topic_reviewers` | `Council` | `councils` |
| `CouncilMember` | `council_members` | `CouncilTopic` | `council_topics` |
| `Report` | `reports` | `Score` | `scores` |
| `Result` | `results` | `Announcement` | `announcements` |

Các enum (`UserRole`, `UserStatus`, `TopicStatus`, `PeriodType`, `PeriodStatus`, `GroupStatus`, `RegistrationStatus`, `ReportStatus`, `CouncilStatus`, `CouncilRole`, `AnnouncementScope`, `AnnouncementStatus`) là danh sách giá trị hợp lệ tương ứng với cột `ENUM` trong SQL.

## 5. Repository: truy cập database

Mỗi interface kế thừa `JpaRepository<Entity, Long>` nên có sẵn các thao tác tìm/thêm/sửa/xóa. Các truy vấn đặc thù được khai báo bằng tên hàm `findBy...` hoặc `@Query`.

- `UserRepository`, `DepartmentRepository`, `RegistrationPeriodRepository`: tài khoản, bộ môn và đợt.
- `TopicRepository`, `TopicSupervisorRepository`: đề tài và GVHD.
- `StudentGroupRepository`, `GroupMemberRepository`, `TopicRegistrationRepository`: nhóm, thành viên và đăng ký.
- `ReportRepository`, `TopicReviewerRepository`: báo cáo và phân công/điểm phản biện.
- `CouncilRepository`, `CouncilMemberRepository`, `CouncilTopicRepository`, `ScoreRepository`, `ResultRepository`: hội đồng, thành viên, đề tài, điểm và kết quả.
- `AnnouncementRepository`: thông báo.
- `ScoreRepository` đọc view `v_registration_avg_score` qua projection `RegistrationScoreSummary`.

## 6. DTO: dữ liệu giữa form, service và trang

- Các lớp `*Form` (`UserForm`, `PasswordChangeForm`, `TopicForm`, `RegistrationPeriodForm`, `CouncilForm`, `CouncilMemberForm`, `TopicRegistrationForm`, `ReviewScoreForm`, `CouncilResultForm`, `AnnouncementForm`) nhận dữ liệu nhập và dùng Bean Validation.
- Các lớp `*View`, `*Option`, `*Summary` (`TopicView`, `StudentGroupView`, `StudentRegistrationView`, `StudentReportView`, `StudentResultView`, `CouncilSummaryView`, `CouncilDetailView`, `CouncilMemberView`, `CouncilRegistrationOption`, `CouncilWorkView`, `ReviewerTaskView`, `ReviewAssignmentView`, `LecturerOption`, `AnnouncementView`, `RegistrationScoreSummary`) chỉ mang dữ liệu cần hiển thị; chúng tránh để trang HTML phải đọc quan hệ JPA lazy.
- `ReportDownload` chỉ giữ đường dẫn đã kiểm tra và tên tệp tải xuống; đường dẫn ổ đĩa không hiển thị trên giao diện.

## 7. Template và database

- `templates/admin`, `templates/head`, `templates/lecturer`, `templates/student`: trang theo vai trò; `home.html` chứa liên kết chức năng; `login.html` là trang đăng nhập; `error/403.html` báo thiếu quyền.
- Mỗi trang HTML nằm trong thư mục theo vai trò và theo chức năng: `admin/users`, `periods`, `head/topics`, `head/registrations`, `head/reviewers`, `head/councils`, `lecturer/topics`, `lecturer/reviews`, `lecturer/councils`, `student/groups`, `student/registrations`, `student/reports`, `student/results`, `announcements`.
- `static/css/style.css`: kiểu dáng dùng chung.
- `student_topic_management_v2.sql`: tạo schema gốc, dữ liệu mẫu và view điểm trung bình. Script có `DROP DATABASE`; chỉ dùng khi chủ ý tạo lại database.
- `database/migrations/002_add_reviewer_scores.sql`: chạy **một lần sau SQL gốc** để thêm điểm/nhận xét GVPB vào `topic_reviewers`. Ứng dụng đặt `ddl-auto=none` nên không tự áp dụng migration.

## 8. Một luồng mẫu để tập đọc code

Khi nhóm trưởng nộp báo cáo, mở `StudentReportController` để thấy URL/form, xem `StudentReportService` để hiểu quyền và quy tắc, tiếp đến `GroupMemberRepository`/`TopicRegistrationRepository` để thấy dữ liệu được truy vấn, `Report` để hiểu ánh xạ bảng, và `templates/student/reports/page.html` để xem nội dung trình bày.

Khi giảng viên phản biện, xem `LecturerReviewController` và `ReviewerService`; phần tải báo cáo dùng cùng `StudentReportService` nhưng xác nhận tài khoản nằm trong danh sách GVPB hoặc hội đồng trước khi trả tệp. Luồng chấm điểm nằm ở `LecturerCouncilController` → `CouncilScoringService` → `ScoreRepository`/`ResultRepository` → `templates/lecturer/councils/work.html`.

