-- Chi dung de lam sach database demo tren may local.
-- Script xoa du lieu nghiep vu va tai khoan mau, giu lai user admin va cac bo mon.
-- Xac nhan dung schema truoc khi chay: student_topic_management.

USE student_topic_management;

SELECT DATABASE() AS database_being_reset;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE announcements;
TRUNCATE TABLE council_members;
TRUNCATE TABLE council_topics;
TRUNCATE TABLE councils;
TRUNCATE TABLE group_members;
TRUNCATE TABLE registration_periods;
TRUNCATE TABLE reports;
TRUNCATE TABLE results;
TRUNCATE TABLE scores;
TRUNCATE TABLE student_groups;
TRUNCATE TABLE topic_registrations;
TRUNCATE TABLE topic_reviewers;
TRUNCATE TABLE topic_supervisors;
TRUNCATE TABLE topics;

DELETE FROM users WHERE username <> 'admin';

SET FOREIGN_KEY_CHECKS = 1;

SELECT id, username, full_name, role, status
FROM users
ORDER BY id;
