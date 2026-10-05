package vn.edu.demo.topicmanagement.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByTopicRegistrationId(Long registrationId);
}
