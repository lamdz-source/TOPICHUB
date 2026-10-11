# TOPICHUB — giao diện hiện đại (UI Modern)

## Những thay đổi

- Đăng nhập theo bố cục 2 cột, đáp ứng màn hình nhỏ.
- Menu dọc theo vai trò (Admin, Trưởng khoa, Giảng viên, Sinh viên), thanh công cụ phía trên, dấu trang đang mở.
- Dashboard thẻ chức năng, quy trình tổng quan.
- Bảng cuộn ngang trên thiết bị nhỏ, ô tìm kiếm nhanh (chỉ lọc các dòng đã tải trên trang, không thay thế tìm kiếm phía máy chủ).
- Biểu mẫu, nhãn trạng thái, cảnh báo, nút và thông báo đồng bộ.
- Nút đổi sáng/tối, nhớ lựa chọn ở trình duyệt. Menu mobile đóng/mở qua nút hamburger.
- Giao diện sử dụng CSS/JS/SVG nội bộ, không cần Internet hoặc CDN.

## Cách cập nhật an toàn

1. **Sao lưu** thư mục `E:\lam2\topicmanagement` và dữ liệu MySQL/MariaDB nếu cần.
2. Dừng Spring Boot trong VS Code bằng `Ctrl+C`.
3. Giải nén `TOPICHUB_UI_MODERN_PATCH.zip` trực tiếp vào **`E:\lam2`** (thư mục chứa `topicmanagement`), cho phép thay thế tệp trùng tên.
4. Tại Terminal VS Code:

   ```powershell
   cd E:\lam2\topicmanagement
   .\mvnw.cmd spring-boot:run
   ```
5. Mở `http://localhost:8080/login`; dùng `Ctrl+F5` để bỏ cache CSS/JS cũ.

**Quan trọng:** Patch chỉ thay template HTML, CSS và bổ sung JS/hướng dẫn; không chỉnh Controller, Service, Entity, cơ sở dữ liệu hay cấu hình kết nối. **Không chạy lại file SQL khởi tạo**.

Nếu bạn giải nén từ bản FULL, cũng giải nén vào `E:\lam2`, nhưng hãy cẩn thận với file cũ, dữ liệu upload hoặc cấu hình địa phương. Với dự án đã vận hành, nên dùng bản PATCH.

## Các bước kiểm thử

1. Đăng nhập mỗi vai trò và xem menu tương ứng.
2. Từ Dashboard, mở các trang theo vai trò và dùng nút Đăng xuất.
3. Tại Trưởng khoa, mở `/periods`, `/head/topics`, `/head/councils` và thử thao tác nghiệp vụ **với dữ liệu thử nghiệm**.
4. Tại Giảng viên, mở `/lecturer/reviews`, `/lecturer/council-work`.
5. Tại Sinh viên, mở `/student/groups`, `/student/reports`, `/student/results`.
6. Thu nhỏ cửa sổ về chiều rộng ~390px: menu sidebar phải mở bằng nút ☰ và đóng bằng nền mờ/ESC.
7. Kiểm tra nút chế độ tối trên máy tính, phím Tab, và bảng có thể cuộn ngang trên mobile.

## Giới hạn kiểm thử

Đã đối chiếu các `th:href`, `th:action`, binding Thymeleaf gốc của **26 màn hình nghiệp vụ**: không đổi. Đã render tĩnh và kiểm tra click menu mobile, thay đổi chế độ tối trên Chromium. Chưa chạy một build Spring Boot hoàn chỉnh với JDK 25 và MariaDB của máy người dùng; bạn cần kiểm thử trực tiếp sau khi cập nhật.
