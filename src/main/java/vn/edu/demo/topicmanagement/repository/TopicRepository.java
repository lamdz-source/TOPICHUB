package vn.edu.demo.topicmanagement.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.Topic;
import vn.edu.demo.topicmanagement.entity.TopicStatus;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    boolean existsByRegistrationPeriod_Id(Long registrationPeriodId);

    // Đề tài đã công bố (trang /topics)
    @Query("select t from Topic t join fetch t.department join fetch t.createdBy "
         + "where t.status = :status order by t.id")
    List<Topic> findByStatusWithDetails(@Param("status") TopicStatus status);

    // Đề tài của 1 giảng viên: do họ tạo HOẶC họ là một trong các GVHD
    @Query("select t from Topic t join fetch t.department join fetch t.createdBy join fetch t.registrationPeriod "
         + "where t.createdBy.username = :username "
         + "or t.id in (select ts.topic.id from TopicSupervisor ts where ts.lecturer.username = :username) "
         + "order by t.id desc")
    List<Topic> findByLecturer(@Param("username") String username);

    // Tất cả đề tài (cho Trưởng khoa)
    @Query("select t from Topic t join fetch t.department join fetch t.createdBy join fetch t.registrationPeriod "
         + "order by t.id desc")
    List<Topic> findAllWithDetails();

    List<Topic> findByStatus(TopicStatus status);

    @Query("select t from Topic t join fetch t.department join fetch t.registrationPeriod p "
         + "where t.status = :topicStatus and p.status = :periodStatus "
         + "and p.studentStart <= :now and p.studentEnd >= :now order by t.id desc")
    List<Topic> findAvailableForStudents(@Param("topicStatus") TopicStatus topicStatus,
                                         @Param("periodStatus") PeriodStatus periodStatus,
                                         @Param("now") LocalDateTime now);
}
