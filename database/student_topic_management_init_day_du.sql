-- BAN KHOI TAO CSDL DAY DU TU DU AN LAM2
-- CANH BAO: script nay DROP DATABASE student_topic_management, XOA TOAN BO DU LIEU CU.
-- CHI DUNG TREN CSDL MOI / BAN SAO THU NGHIEM.
-- Neu da co CSDL, KHONG chay script nay; chi chay migration 002_add_reviewer_scores.sql (mot lan).

-- =========================================================
-- DATABASE: HỆ THỐNG QUẢN LÝ ĐỀ TÀI SINH VIÊN  (BẢN V2)

DROP DATABASE IF EXISTS student_topic_management;
CREATE DATABASE student_topic_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE student_topic_management;

-- =========================================================
-- 1. DEPARTMENTS (Bộ môn)
-- =========================================================
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
) ENGINE=InnoDB;

-- =========================================================
-- 2. USERS (Tài khoản: admin, trưởng khoa, giảng viên, sinh viên)
-- password lưu dạng BCrypt hash, KHÔNG lưu mật khẩu thô
-- =========================================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    phone VARCHAR(20),
    role ENUM('ADMIN', 'HEAD', 'LECTURER', 'STUDENT') NOT NULL,   -- [1] thêm HEAD
    student_code VARCHAR(50) UNIQUE,
    lecturer_code VARCHAR(50) UNIQUE,
    department_id BIGINT,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- [4] Mã SV chỉ dành cho sinh viên; mã GV chỉ dành cho HEAD/LECTURER
    CONSTRAINT chk_users_student_code
        CHECK (role = 'STUDENT' OR student_code IS NULL),
    CONSTRAINT chk_users_lecturer_code
        CHECK (role IN ('HEAD', 'LECTURER') OR lecturer_code IS NULL),

    CONSTRAINT fk_users_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 3. REGISTRATION_PERIODS (Đợt đăng ký - do Trưởng khoa tạo)
-- Giai đoạn 1: GV đăng ký đề tài (lecturer_start -> lecturer_end)
-- Giai đoạn 2: SV đăng ký đề tài (student_start -> student_end)
-- =========================================================
CREATE TABLE registration_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type ENUM('COURSE', 'RESEARCH', 'TLCN', 'KLTN') NOT NULL,

    lecturer_start DATETIME NOT NULL,
    lecturer_end DATETIME NOT NULL,

    student_start DATETIME NOT NULL,
    student_end DATETIME NOT NULL,

    review_deadline DATETIME NULL,   -- hạn GVPB nộp điểm (chỉ TLCN, KLTN)
    defense_date DATETIME NULL,      -- ngày báo cáo hội đồng (chỉ KLTN)

    status ENUM('DRAFT', 'OPEN', 'CLOSED') NOT NULL DEFAULT 'DRAFT',

    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- [4] Ràng buộc thời gian
    CONSTRAINT chk_period_lecturer_time
        CHECK (lecturer_end > lecturer_start),
    CONSTRAINT chk_period_student_time
        CHECK (student_end > student_start),
    CONSTRAINT chk_period_two_phases
        CHECK (student_start >= lecturer_end),   -- giai đoạn SV bắt đầu sau giai đoạn GV
    CONSTRAINT chk_period_review_deadline
        CHECK (type IN ('TLCN', 'KLTN') OR review_deadline IS NULL),
    CONSTRAINT chk_period_defense_date
        CHECK (type = 'KLTN' OR defense_date IS NULL),

    CONSTRAINT fk_period_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 4. TOPICS (Đề tài)
-- =========================================================
CREATE TABLE topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,

    department_id BIGINT NOT NULL,
    registration_period_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,

    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'PUBLISHED')
        NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,             -- [3] lý do từ chối

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_topics_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_topics_period
        FOREIGN KEY (registration_period_id)
        REFERENCES registration_periods(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_topics_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 5. TOPIC_SUPERVISORS (GVHD: 1 đến 2 GV / đề tài)
-- Quy tắc "tối đa 2 GV" kiểm tra ở tầng Service
-- =========================================================
CREATE TABLE topic_supervisors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_topic_supervisor
        UNIQUE (topic_id, lecturer_id),

    CONSTRAINT fk_topic_supervisors_topic
        FOREIGN KEY (topic_id)
        REFERENCES topics(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_topic_supervisors_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 6. STUDENT_GROUPS (Nhóm sinh viên)
-- =========================================================
CREATE TABLE student_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    group_leader_id BIGINT NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_groups_leader
        FOREIGN KEY (group_leader_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 7. GROUP_MEMBERS (Thành viên nhóm, tối đa 3 - kiểm tra ở Service)
-- Mỗi SV chỉ thuộc một nhóm (UNIQUE student_id)
-- =========================================================
CREATE TABLE group_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_student_in_group
        UNIQUE (student_id),

    CONSTRAINT uq_group_student
        UNIQUE (group_id, student_id),

    CONSTRAINT fk_group_members_group
        FOREIGN KEY (group_id)
        REFERENCES student_groups(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_group_members_student
        FOREIGN KEY (student_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 8. TOPIC_REGISTRATIONS (Nhóm đăng ký đề tài)
-- =========================================================
CREATE TABLE topic_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,

    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')
        NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500) NULL,             -- [3] lý do từ chối

    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at DATETIME NULL,

    -- Mỗi nhóm chỉ đăng ký đúng 1 đề tài
    CONSTRAINT uq_group_topic_registration
        UNIQUE (group_id),

    CONSTRAINT fk_topic_registrations_group
        FOREIGN KEY (group_id)
        REFERENCES student_groups(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_topic_registrations_topic
        FOREIGN KEY (topic_id)
        REFERENCES topics(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 8b. TOPIC_REVIEWERS (GVPB - giảng viên phản biện)   [2] BẢNG MỚI
-- GVPB không được trùng với GVHD của đề tài đó (kiểm tra ở Service)
-- =========================================================
CREATE TABLE topic_reviewers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_registration_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_score DECIMAL(4,2) NULL,
    review_comment TEXT NULL,
    review_submitted_at DATETIME NULL,

    CONSTRAINT chk_topic_reviewers_review_score_0_10
        CHECK (review_score IS NULL OR (review_score >= 0 AND review_score <= 10)),

    CONSTRAINT uq_reviewer_registration
        UNIQUE (topic_registration_id, lecturer_id),

    CONSTRAINT fk_topic_reviewers_registration
        FOREIGN KEY (topic_registration_id)
        REFERENCES topic_registrations(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_topic_reviewers_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 9. COUNCILS (Hội đồng phản biện)
-- =========================================================
CREATE TABLE councils (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    registration_period_id BIGINT NOT NULL,
    defense_date DATETIME NULL,
    room VARCHAR(100),
    status ENUM('DRAFT', 'ACTIVE', 'COMPLETED')
        NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT fk_councils_period
        FOREIGN KEY (registration_period_id)
        REFERENCES registration_periods(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 10. COUNCIL_MEMBERS (3-5 GV, đúng 1 chủ tịch + 1 thư ký - kiểm tra ở Service)
-- =========================================================
CREATE TABLE council_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    council_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,
    role ENUM('CHAIRMAN', 'SECRETARY', 'MEMBER') NOT NULL,

    CONSTRAINT uq_council_lecturer
        UNIQUE (council_id, lecturer_id),

    CONSTRAINT fk_council_members_council
        FOREIGN KEY (council_id)
        REFERENCES councils(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_council_members_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 11. COUNCIL_TOPICS (Đề tài được phân vào hội đồng nào)
-- =========================================================
CREATE TABLE council_topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    council_id BIGINT NOT NULL,
    topic_registration_id BIGINT NOT NULL,

    -- Một đăng ký đề tài chỉ thuộc một hội đồng
    CONSTRAINT uq_registration_council
        UNIQUE (topic_registration_id),

    CONSTRAINT uq_council_topic
        UNIQUE (council_id, topic_registration_id),

    CONSTRAINT fk_council_topics_council
        FOREIGN KEY (council_id)
        REFERENCES councils(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_council_topics_registration
        FOREIGN KEY (topic_registration_id)
        REFERENCES topic_registrations(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 12. REPORTS (Báo cáo - chỉ nhóm trưởng được nộp, kiểm tra ở Service)
-- =========================================================
CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_registration_id BIGINT NOT NULL,
    submitted_by BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,

    submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    status ENUM('SUBMITTED', 'REVIEWED')
        NOT NULL DEFAULT 'SUBMITTED',

    -- Mỗi đăng ký có một báo cáo
    CONSTRAINT uq_report_registration
        UNIQUE (topic_registration_id),

    CONSTRAINT fk_reports_registration
        FOREIGN KEY (topic_registration_id)
        REFERENCES topic_registrations(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_reports_submitter
        FOREIGN KEY (submitted_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 13. SCORES (Điểm thành phần của từng GV)
-- GV không được chấm đề tài mình hướng dẫn -> kiểm tra ở Service
-- =========================================================
CREATE TABLE scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    council_topic_id BIGINT NOT NULL,
    lecturer_id BIGINT NOT NULL,

    score DECIMAL(4,2) NOT NULL,
    comment TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Một GV chỉ chấm một lần cho một đề tài trong hội đồng
    CONSTRAINT uq_score_lecturer_topic
        UNIQUE (council_topic_id, lecturer_id),

    CONSTRAINT chk_score_range
        CHECK (score >= 0 AND score <= 10),

    CONSTRAINT fk_scores_council_topic
        FOREIGN KEY (council_topic_id)
        REFERENCES council_topics(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_scores_lecturer
        FOREIGN KEY (lecturer_id)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 14. RESULTS (Kết quả cuối - chủ tịch hội đồng tổng hợp)
-- =========================================================
CREATE TABLE results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_registration_id BIGINT NOT NULL,

    final_score DECIMAL(4,2) NOT NULL,
    result VARCHAR(100),
    council_comment TEXT NULL,                   -- [3] nhận xét tổng hợp

    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at DATETIME NULL,

    CONSTRAINT uq_result_registration
        UNIQUE (topic_registration_id),

    CONSTRAINT chk_final_score_range
        CHECK (final_score >= 0 AND final_score <= 10),

    CONSTRAINT fk_results_registration
        FOREIGN KEY (topic_registration_id)
        REFERENCES topic_registrations(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 15. ANNOUNCEMENTS (Thông báo của trường / khoa)
-- =========================================================
CREATE TABLE announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,

    created_by BIGINT NOT NULL,

    scope ENUM('SCHOOL', 'DEPARTMENT') NOT NULL DEFAULT 'SCHOOL',
    department_id BIGINT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME NULL,

    status ENUM('DRAFT', 'PUBLISHED')
        NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT fk_announcements_creator
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_announcements_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- VIEW: điểm trung bình cộng các điểm thành phần của mỗi đăng ký   [5]
-- (đề: "Điểm cuối cùng = trung bình cộng các điểm thành phần")
-- =========================================================
CREATE VIEW v_registration_avg_score AS
SELECT
    ct.topic_registration_id,
    COUNT(s.id)              AS score_count,
    ROUND(AVG(s.score), 2)   AS avg_score
FROM council_topics ct
JOIN scores s ON s.council_topic_id = ct.id
GROUP BY ct.topic_registration_id;

-- =========================================================
-- DỮ LIỆU MẪU  [6]
-- Mật khẩu của TẤT CẢ tài khoản mẫu là:  password
-- (hash BCrypt, dùng được với BCryptPasswordEncoder của Spring Security)
-- =========================================================

INSERT INTO departments (id, name, description) VALUES
(1, 'Công nghệ phần mềm', 'Bộ môn Công nghệ phần mềm'),
(2, 'Hệ thống thông tin', 'Bộ môn Hệ thống thông tin'),
(3, 'Mạng máy tính', 'Bộ môn Mạng máy tính và truyền thông');

-- id 1: admin | 2: trưởng khoa | 3-8: giảng viên | 9-16: sinh viên
INSERT INTO users
(id, username, password, full_name, email, role, student_code, lecturer_code, department_id) VALUES
(1,  'admin',   '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Quản trị hệ thống',  'admin@demo.edu.vn',   'ADMIN',    NULL, NULL, NULL),
(2,  'head01',  '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Nguyễn Văn Khoa',    'head@demo.edu.vn',    'HEAD',     NULL, 'GV000', 1),
(3,  'gv01',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Trần Minh An',       'gv01@demo.edu.vn',    'LECTURER', NULL, 'GV001', 1),
(4,  'gv02',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Lê Thị Bích',        'gv02@demo.edu.vn',    'LECTURER', NULL, 'GV002', 2),
(5,  'gv03',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Phạm Quốc Cường',    'gv03@demo.edu.vn',    'LECTURER', NULL, 'GV003', 1),
(6,  'gv04',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Võ Thanh Dũng',      'gv04@demo.edu.vn',    'LECTURER', NULL, 'GV004', 2),
(7,  'gv05',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Đặng Hoàng Em',      'gv05@demo.edu.vn',    'LECTURER', NULL, 'GV005', 3),
(8,  'gv06',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Bùi Thu Phương',     'gv06@demo.edu.vn',    'LECTURER', NULL, 'GV006', 3),
(9,  'sv01',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Nguyễn Anh Khoa',    'sv01@demo.edu.vn',    'STUDENT',  'SV001', NULL, 1),
(10, 'sv02',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Trần Bảo Ngọc',      'sv02@demo.edu.vn',    'STUDENT',  'SV002', NULL, 1),
(11, 'sv03',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Lê Minh Tuấn',       'sv03@demo.edu.vn',    'STUDENT',  'SV003', NULL, 1),
(12, 'sv04',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Phạm Gia Hân',       'sv04@demo.edu.vn',    'STUDENT',  'SV004', NULL, 2),
(13, 'sv05',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Võ Đức Huy',         'sv05@demo.edu.vn',    'STUDENT',  'SV005', NULL, 2),
(14, 'sv06',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Đỗ Thùy Linh',       'sv06@demo.edu.vn',    'STUDENT',  'SV006', NULL, 3),
(15, 'sv07',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Hồ Quang Minh',      'sv07@demo.edu.vn',    'STUDENT',  'SV007', NULL, 3),
(16, 'sv08',    '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Ngô Thanh Sơn',      'sv08@demo.edu.vn',    'STUDENT',  'SV008', NULL, 3);

-- Đợt 1: KLTN đang mở. Đợt 2: môn học, còn nháp (không có hạn GVPB / ngày bảo vệ)
INSERT INTO registration_periods
(id, name, type, lecturer_start, lecturer_end, student_start, student_end,
 review_deadline, defense_date, status, created_by) VALUES
(1, 'Đợt đăng ký KLTN HK1 2026-2027', 'KLTN',
    '2026-09-01 00:00:00', '2026-09-15 23:59:59',
    '2026-09-16 00:00:00', '2026-09-30 23:59:59',
    '2026-12-10 17:00:00', '2026-12-20 08:00:00', 'OPEN', 2),
(2, 'Đợt đăng ký đề tài môn học HK1 2026-2027', 'COURSE',
    '2026-10-10 00:00:00', '2026-10-17 23:59:59',
    '2026-10-18 00:00:00', '2026-10-25 23:59:59',
    NULL, NULL, 'DRAFT', 2);

INSERT INTO topics
(id, title, description, department_id, registration_period_id, created_by, status, reject_reason) VALUES
(1, 'Xây dựng website quản lý đề tài sinh viên', 'Ứng dụng web Spring Boot quản lý quy trình đăng ký, phản biện đề tài.', 1, 1, 3, 'PUBLISHED', NULL),
(2, 'Chatbot hỗ trợ tư vấn tuyển sinh',          'Chatbot trả lời câu hỏi tuyển sinh dựa trên cơ sở tri thức.',            2, 1, 4, 'PUBLISHED', NULL),
(3, 'Hệ thống giám sát lưu lượng mạng nội bộ',   'Thu thập và trực quan hóa lưu lượng mạng của khoa.',                     3, 1, 7, 'PUBLISHED', NULL),
(4, 'Ứng dụng thương mại điện tử đa nền tảng',   'Website bán hàng kèm giỏ hàng và thanh toán.',                           1, 1, 3, 'PENDING',   NULL),
(5, 'Phần mềm quản lý quán cà phê',              'Đề tài chưa rõ phạm vi.',                                                 1, 1, 5, 'REJECTED',  'Nội dung chưa rõ ràng, cần mô tả cụ thể chức năng.');

-- GVHD: mỗi đề tài 1-2 GV
INSERT INTO topic_supervisors (topic_id, lecturer_id, is_primary) VALUES
(1, 3, TRUE), (1, 4, FALSE),
(2, 4, TRUE),
(3, 7, TRUE),
(4, 3, TRUE),
(5, 5, TRUE);

INSERT INTO student_groups (id, name, group_leader_id) VALUES
(1, 'Nhóm 1', 9),
(2, 'Nhóm 2', 12),
(3, 'Nhóm 3', 14);

INSERT INTO group_members (group_id, student_id) VALUES
(1, 9), (1, 10), (1, 11),
(2, 12), (2, 13),
(3, 14), (3, 15), (3, 16);

INSERT INTO topic_registrations (id, group_id, topic_id, status, approved_at) VALUES
(1, 1, 1, 'APPROVED', '2026-10-01 09:00:00'),
(2, 2, 2, 'APPROVED', '2026-10-01 09:30:00'),
(3, 3, 3, 'PENDING',  NULL);

-- GVPB
INSERT INTO topic_reviewers (topic_registration_id, lecturer_id) VALUES
(1, 5),
(2, 6);

-- Hội đồng 1: 4 GV (5 chủ tịch, 6 thư ký, 7 và 8 ủy viên)
INSERT INTO councils (id, name, registration_period_id, defense_date, room, status) VALUES
(1, 'Hội đồng 1', 1, '2026-12-20 08:00:00', 'A5.01', 'ACTIVE');

INSERT INTO council_members (council_id, lecturer_id, role) VALUES
(1, 5, 'CHAIRMAN'),
(1, 6, 'SECRETARY'),
(1, 7, 'MEMBER'),
(1, 8, 'MEMBER');

INSERT INTO council_topics (id, council_id, topic_registration_id) VALUES
(1, 1, 1),
(2, 1, 2);

INSERT INTO reports (topic_registration_id, submitted_by, file_name, file_path) VALUES
(1, 9,  'baocao_nhom1.pdf', 'uploads/reports/baocao_nhom1.pdf'),
(2, 12, 'baocao_nhom2.pdf', 'uploads/reports/baocao_nhom2.pdf');

-- Điểm: GV 3,4 là GVHD nên không chấm đề tài của mình
INSERT INTO scores (council_topic_id, lecturer_id, score, comment) VALUES
(1, 5, 8.50, 'Hệ thống hoàn thiện, đúng yêu cầu.'),
(1, 6, 8.00, 'Giao diện ổn, cần bổ sung kiểm thử.'),
(1, 7, 9.00, 'Trình bày rõ ràng.'),
(1, 8, 8.50, 'Tốt.'),
(2, 5, 7.50, 'Chatbot hoạt động nhưng dữ liệu còn ít.'),
(2, 6, 8.00, 'Khá.'),
(2, 7, 7.50, 'Cần cải thiện độ chính xác.'),
(2, 8, 8.00, 'Khá.');

-- final_score = trung bình cộng: (8.5+8+9+8.5)/4 = 8.50 ; (7.5+8+7.5+8)/4 = 7.75
INSERT INTO results
(topic_registration_id, final_score, result, council_comment, published, published_at) VALUES
(1, 8.50, 'Giỏi', 'Đề tài hoàn thành tốt.', TRUE,  '2026-12-21 10:00:00'),
(2, 7.75, 'Khá',  'Đề tài đạt yêu cầu.',    FALSE, NULL);

INSERT INTO announcements
(title, content, created_by, scope, department_id, published_at, status) VALUES
('Mở đăng ký đề tài KLTN HK1 2026-2027',
 'Khoa CNTT thông báo mở đợt đăng ký đề tài KLTN. Sinh viên xem lịch chi tiết trên hệ thống.',
 2, 'SCHOOL', NULL, '2026-09-01 08:00:00', 'PUBLISHED'),
('Họp bộ môn Công nghệ phần mềm',
 'Các GV bộ môn họp duyệt danh sách đề tài vào thứ Sáu tuần này.',
 3, 'DEPARTMENT', 1, NULL, 'DRAFT');

-- =========================================================
-- KIỂM TRA
-- =========================================================
SHOW TABLES;   -- phải thấy 16 bảng + 1 view (v_registration_avg_score)

-- Kiểm tra điểm trung bình (phải khớp bảng results: 8.50 và 7.75)
SELECT * FROM v_registration_avg_score;

-- Kiểm tra quy tắc "GV không chấm đề tài mình hướng dẫn": kết quả phải RỖNG
SELECT s.id AS score_id, s.lecturer_id, ct.topic_registration_id
FROM scores s
JOIN council_topics ct   ON ct.id = s.council_topic_id
JOIN topic_registrations tr ON tr.id = ct.topic_registration_id
JOIN topic_supervisors ts   ON ts.topic_id = tr.topic_id
                           AND ts.lecturer_id = s.lecturer_id;
