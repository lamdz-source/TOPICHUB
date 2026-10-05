package vn.edu.demo.topicmanagement.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.TopicSupervisor;

public interface TopicSupervisorRepository extends JpaRepository<TopicSupervisor, Long> {

    // Lấy GVHD của NHIỀU đề tài trong 1 câu truy vấn (tránh gọi database lặp đi lặp lại cho từng đề tài)
    @Query("select ts from TopicSupervisor ts join fetch ts.lecturer "
         + "where ts.topic.id in :topicIds order by ts.primarySupervisor desc, ts.id")
    List<TopicSupervisor> findByTopicIds(@Param("topicIds") Collection<Long> topicIds);

    List<TopicSupervisor> findByTopic_IdAndLecturer_Username(Long topicId, String username);

    // Xóa toàn bộ GVHD của 1 đề tài (dùng khi sửa đề tài rồi ghi lại danh sách mới).
    // flushAutomatically: ghi các thay đổi đang chờ xuống DB trước khi xóa.
    @Modifying(flushAutomatically = true)
    @Query("delete from TopicSupervisor ts where ts.topic.id = :topicId")
    void deleteByTopic(@Param("topicId") Long topicId);
}
