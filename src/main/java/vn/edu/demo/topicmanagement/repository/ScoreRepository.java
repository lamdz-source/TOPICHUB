package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.dto.RegistrationScoreSummary;
import vn.edu.demo.topicmanagement.entity.Score;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByCouncilTopicId(Long councilTopicId);
    Optional<Score> findByCouncilTopicIdAndLecturerId(Long councilTopicId, Long lecturerId);

    @Query(value = "select topic_registration_id as topicRegistrationId, score_count as scoreCount, avg_score as avgScore "
            + "from v_registration_avg_score where topic_registration_id = :registrationId", nativeQuery = true)
    Optional<RegistrationScoreSummary> findAverageForRegistration(@Param("registrationId") Long registrationId);
}
