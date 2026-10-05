package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.Announcement;
import vn.edu.demo.topicmanagement.entity.AnnouncementStatus;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByStatusOrderByPublishedAtDesc(AnnouncementStatus status);
    List<Announcement> findByStatusAndDepartmentIdOrderByPublishedAtDesc(AnnouncementStatus status, Long departmentId);
    List<Announcement> findByCreatedByUsernameOrderByCreatedAtDesc(String username);
}
