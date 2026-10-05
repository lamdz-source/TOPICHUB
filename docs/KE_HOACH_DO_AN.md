# Kế hoạch hoàn thiện đồ án quản lý đề tài sinh viên

Tài liệu này đối chiếu đặc tả được cung cấp với code và database hiện có. Phân công bên dưới là đề xuất cho nhóm 5 người; đổi tên thành viên và MSSV trước khi đưa vào báo cáo.

## 1. Phạm vi nghiệp vụ

Luồng chính:

1. Trưởng khoa tạo đợt, quy định giai đoạn GV và SV; hạn phản biện chỉ áp dụng TLCN/KLTN, ngày hội đồng chỉ áp dụng KLTN.
2. Giảng viên đăng ký đề tài thuộc bộ môn trong giai đoạn GV. Trưởng khoa duyệt hoặc từ chối, sau đó công bố.
3. Sinh viên lập nhóm tối đa 3 người, mỗi SV thuộc tối đa một nhóm; nhóm trưởng đăng ký một đề tài đã công bố trong giai đoạn SV.
4. Trưởng khoa duyệt đăng ký nhóm. Nhóm trưởng nộp báo cáo.
5. Trưởng khoa phân công phản biện và lập hội đồng. Hội đồng có 3–5 GV, đúng một chủ tịch và một thư ký.
6. Thành viên hội đồng chấm điểm. Không cho GVHD chấm đề tài mình hướng dẫn. Điểm cuối là trung bình cộng; chủ tịch tổng hợp nhận xét và công bố.
7. Người dùng xem thông báo và kết quả theo quyền được cấp.

## 2. Hiện trạng từ workspace và SQL

| Khu vực | Đã có | Còn cần làm |
|---|---|---|
| CSDL và lớp dữ liệu | SQL có 16 bảng, khóa ngoại/unique/check, dữ liệu mẫu và view điểm TB; có entity/repository Java tương ứng; ERD và từ điển dữ liệu được ghi ở `docs/ERD_VA_TU_DIEN_DU_LIEU.md` | Chưa xác nhận khởi động ứng dụng với toàn bộ schema bằng một lần build/chạy hoàn chỉnh; các quy tắc nhiều bảng vẫn cần service |
| Tài khoản và bảo mật | Đăng nhập Spring Security, BCrypt, role ADMIN/HEAD/LECTURER/STUDENT trong DB; admin tạo/sửa/khóa/mở khóa và xóa tài khoản chưa được tham chiếu; người dùng tự đổi mật khẩu sau khi xác nhận mật khẩu hiện tại; tài khoản INACTIVE bị chặn đăng nhập | Rà soát quyền theo dữ liệu sở hữu và hoàn thiện trang lỗi/thông báo |
| Đợt đăng ký | Entity/enum, repository, tạo đợt, kiểm tra hai giai đoạn và trường theo loại, mở/đóng đợt, giao diện HEAD; service lọc đề tài theo giai đoạn SV | Bổ sung sửa/chi tiết nếu cần; luồng quản lý nâng cao chưa có |
| Đề tài GV | Entity, repository, service, form, controller và trang xem/đăng ký/sửa/xóa an toàn; chỉ người tạo được xóa đề tài PENDING/REJECTED chưa có đăng ký SV; HEAD duyệt, từ chối, công bố | Rà soát quyền HEAD theo bộ môn; kiểm tra trạng thái/thời gian và dữ liệu lỗi |
| Nhóm SV | Entity/repository/service/controller/giao diện tạo nhóm và tham gia nhóm đang nhận thành viên; kiểm tra tối đa 3 SV và một nhóm/SV | Chưa có luồng mời thành viên bằng mã/duyệt yêu cầu tham gia; hiện SV chọn nhóm đang có chỗ |
| Đăng ký đề tài SV | Entity/repository/service/controller/giao diện: trưởng nhóm đăng ký đề tài đã công bố trong giai đoạn SV; HEAD duyệt/từ chối; SV xem trạng thái; nhóm được gửi lại sau khi bị từ chối | Mở rộng luồng sau khi đăng ký được duyệt (ví dụ nộp báo cáo) |
| Báo cáo | Upload/download cho thành viên nhóm; chỉ trưởng nhóm nộp sau khi đăng ký được duyệt; nhận PDF/DOC/DOCX tối đa 10 MB; GVPB/thành viên hội đồng được tải tệp đã phân công | Cần cấu hình lưu trữ bền vững khi deploy |
| Phản biện/hội đồng | HEAD phân công GVPB cho TLCN/KLTN; giảng viên nhập/cập nhật điểm trước hạn; cấm phân GVPB trùng GVHD; HEAD tạo hội đồng, kiểm tra 3–5 GV và chức danh, xếp đề tài có báo cáo/điểm phản biện; GVHD trong hội đồng được miễn chấm đề tài mình hướng dẫn; chủ tịch tổng hợp điểm các thành viên đủ điều kiện; HEAD công bố, SV xem kết quả nhóm mình | Cần kiểm tra các luồng với database; chạy migration thêm các cột điểm GVPB trước khi mở trang phản biện |
| Thông báo | Admin tạo/công bố SCHOOL; HEAD tạo/công bố SCHOOL hoặc DEPARTMENT; người dùng xem thông báo phù hợp bộ môn | Cần kiểm tra lọc phạm vi và cấu hình trên hosting |
| Giao diện chung | Thymeleaf cho đăng nhập, user, topic, periods, nhóm và đăng ký của SV, HEAD duyệt đăng ký | Chưa có layout/navigation chung, councils/results/announcements và xử lý lỗi nhập liệu nhất quán |
| Báo cáo/nộp bài | Yêu cầu báo cáo Word, mã nguồn, deploy hosting free, ZIP đúng tên nhóm | Cần use case, FR/NFR, ERD, data dictionary, ảnh luồng chính, hướng dẫn chạy/deploy; PowerPoint không bắt buộc |

**Lưu ý về database:** file SQL tạo 16 bảng và một view, có dữ liệu minh họa. Dòng đầu script `DROP DATABASE IF EXISTS` sẽ xóa database cũ cùng tên khi chạy lại; chỉ chạy trên database có thể tạo lại. Không đưa mật khẩu MySQL cá nhân vào Git; cấu hình hiện có giá trị mặc định trong `application.properties`, nên chuyển sang biến môi trường và thay mật khẩu nếu đó là mật khẩu thật.

Để lưu điểm phản biện, chạy `database/migrations/002_add_reviewer_scores.sql` một lần sau khi đã tạo schema gốc; ứng dụng không tự chạy migration vì `spring.jpa.hibernate.ddl-auto=none`.

## 3. Phân công đề xuất cho 5 thành viên

Mỗi người phụ trách trọn một module từ entity/repository tới service/controller/view, đồng thời ghi lại quy tắc nghiệp vụ và ảnh màn hình. Một người có thể làm điều phối/tích hợp ngoài module được giao.

| Thành viên | Phạm vi chính | Điểm bàn giao |
|---|---|---|
| Thành viên 1 – điều phối/tích hợp | Khung dự án, đăng nhập/phân quyền, tài khoản; tích hợp nhánh, quản lý cấu hình và deploy | App chạy từ hướng dẫn cài đặt; role/URL rõ; cấu hình secret qua biến môi trường; hướng dẫn triển khai |
| Thành viên 2 | Đợt đăng ký, bộ môn và quản lý đề tài phía GV/HEAD | CRUD đợt, kiểm tra hai giai đoạn và điều kiện theo loại; tạo/sửa/duyệt/từ chối/công bố đề tài; giới hạn 1–2 GVHD |
| Thành viên 3 | Nhóm SV, đăng ký đề tài, duyệt đăng ký | Tạo nhóm/thành viên/trưởng nhóm; tối đa 3, không trùng nhóm; đăng ký một đề tài đã công bố; HEAD duyệt/từ chối |
| Thành viên 4 | Nộp báo cáo, phản biện và hội đồng | Nhóm trưởng nộp tệp; phân GVPB; hội đồng 3–5 GV, một chủ tịch/một thư ký; không phân GVHD chấm đề tài đó |
| Thành viên 5 | Chấm điểm, kết quả, thông báo và hỗ trợ tài liệu | GV hội đồng nhập điểm; điểm TB; chủ tịch tổng hợp/công bố; SV xem kết quả; thông báo; ERD/data dictionary/ảnh giao diện |

### Quy ước để ít xung đột khi code

- Mỗi module sở hữu các thư mục `entity`, `repository`, `service`, `controller`, `dto`, `templates` tương ứng. Không sửa module người khác nếu chưa thống nhất.
- Thống nhất trước tên enum/trạng thái, đường dẫn URL, tên field form và quan hệ JPA; gửi một ví dụ request/form cho người điều phối.
- Dùng nhánh riêng theo module, commit nhỏ có nội dung rõ, merge thường xuyên. Thành viên tích hợp xử lý xung đột và kiểm tra toàn luồng.
- Mọi kiểm tra quyền và quy tắc nghiệp vụ phải đặt ở service; ẩn nút trên giao diện chỉ là hỗ trợ, không thay thế kiểm tra quyền phía server.
- Tạm chia việc theo chức năng; cập nhật bảng phân công báo cáo bằng tên thật, MSSV và khối lượng thực tế sau khi nhóm thống nhất.

## 4. Thứ tự triển khai

### Mốc 1 – Chạy được nền tảng

- Đồng bộ entity JPA với các bảng SQL; bổ sung entity/enum đợt đăng ký còn thiếu.
- Cấu hình MySQL bằng biến môi trường; nhập SQL trên database phù hợp.
- Xác nhận đăng nhập từng role, trạng thái ACTIVE/INACTIVE và menu theo role.

### Mốc 2 – Hoàn tất luồng đề tài

- HEAD tạo đợt; kiểm tra ngày giờ và trường bắt buộc theo COURSE/RESEARCH/TLCN/KLTN.
- GV tạo đề tài trong giai đoạn 1, có 1–2 GVHD; HEAD duyệt/từ chối/công bố.
- Tạo nhóm SV, đăng ký một đề tài trong giai đoạn 2, HEAD chấp nhận/từ chối.

### Mốc 3 – Hoàn tất bảo vệ và kết quả

- Nhóm trưởng nộp báo cáo; gán phản biện và đề tài vào hội đồng.
- Kiểm tra thành viên/hai chức danh bắt buộc, chặn GVHD tự chấm; nhập điểm và tính trung bình.
- Chủ tịch hoàn tất nhận xét/công bố; SV chỉ xem kết quả nhóm mình.

### Mốc 4 – Hoàn thiện bàn giao

- Thông báo trường/khoa; rà soát phân quyền và dữ liệu mẫu.
- Chụp luồng đăng nhập, quản lý đợt, duyệt/công bố đề tài, lập nhóm/đăng ký, nộp báo cáo, chấm điểm và xem kết quả.
- Hoàn tất báo cáo Word theo các chương trong yêu cầu; ghi rõ phần chưa đạt và hướng phát triển.
- Deploy lên một hosting miễn phí, ghi URL và cách cấu hình; gom Word, mã nguồn, sơ đồ/tài nguyên vào thư mục, nén đúng mẫu `GroupXX_MSSV1_MSSV2_MSSV3_MSSV4_MSSV5.zip`.

## 5. Cấu trúc báo cáo Word

1. **Tổng quan:** lý do, mục tiêu, phạm vi, bảng phân công thành viên.
2. **Cơ sở lý thuyết:** MVC trong Spring Boot; mô tả ít nhất ba module/kỹ thuật thật sự dùng (ví dụ Spring MVC, Spring Data JPA, Spring Security, Bean Validation, Thymeleaf).
3. **Đặc tả yêu cầu:** use case diagram; FR theo role/luồng; NFR về bảo mật, khả dụng, tương thích, hiệu năng và sao lưu.
4. **Thiết kế CSDL:** ERD, quan hệ đầy đủ, từ điển dữ liệu và quy tắc toàn vẹn.
5. **Giao diện:** ảnh các flow quan trọng, mỗi ảnh có chú thích và vai trò thao tác.
6. **Kết luận:** kết quả đạt được, ưu/khuyết điểm, hướng phát triển.
7. **Phụ lục và tài liệu tham khảo:** cài đặt, biến môi trường, nhập SQL, tài khoản demo, deploy, slide môn học và nguồn Internet đã dùng.

## 6. Quy tắc nghiệp vụ cần kiểm tra xuyên suốt

- Tạo/sửa đề tài trong đúng thời gian GV; SV đăng ký trong đúng thời gian SV; giai đoạn SV bắt đầu sau giai đoạn GV.
- Đề tài có từ 1 đến 2 GVHD; nhóm có tối đa 3 thành viên; mỗi SV thuộc tối đa một nhóm; một nhóm chỉ có một đăng ký đề tài.
- Chỉ đăng ký đề tài đã công bố và còn phù hợp đợt; chỉ nhóm trưởng được nộp báo cáo.
- Hội đồng có 3–5 thành viên, đúng một chủ tịch và một thư ký; mỗi GV không chấm đề tài mình hướng dẫn.
- Điểm trong 0–10; chỉ công bố kết quả khi đủ điểm theo quy tắc mà nhóm thống nhất và chủ tịch hoàn tất tổng hợp.
- Bảo vệ mọi POST bằng CSRF mặc định của Spring Security; mã hóa mật khẩu; không lộ dữ liệu nhóm/kết quả của nhóm khác.

