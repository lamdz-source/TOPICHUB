package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.Result;

public interface ResultRepository extends JpaRepository<Result, Long> {
    Optional<Result> findByTopicRegistrationId(Long registrationId);
    List<Result> findByPublishedFalseOrderByIdAsc();
    List<Result> findByPublishedTrueOrderByPublishedAtDesc();
}
