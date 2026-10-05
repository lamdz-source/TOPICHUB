package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.TopicReviewer;

public interface TopicReviewerRepository extends JpaRepository<TopicReviewer, Long> {
    List<TopicReviewer> findByTopicRegistrationId(Long registrationId);
    List<TopicReviewer> findByLecturerUsernameOrderByAssignedAtDesc(String username);
    boolean existsByTopicRegistrationIdAndLecturerId(Long registrationId, Long lecturerId);
}
