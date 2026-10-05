package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.CouncilTopic;

public interface CouncilTopicRepository extends JpaRepository<CouncilTopic, Long> {
    List<CouncilTopic> findByCouncilId(Long councilId);
    Optional<CouncilTopic> findByTopicRegistrationId(Long registrationId);

    @Query("select distinct ct from CouncilTopic ct join fetch ct.council c "
         + "join fetch ct.topicRegistration tr join fetch tr.studentGroup join fetch tr.topic "
         + "where c.status = vn.edu.demo.topicmanagement.entity.CouncilStatus.ACTIVE "
         + "and exists (select cm.id from CouncilMember cm where cm.council = c and cm.lecturer.username = :username)")
    List<CouncilTopic> findActiveWorkForLecturer(@Param("username") String username);

}
