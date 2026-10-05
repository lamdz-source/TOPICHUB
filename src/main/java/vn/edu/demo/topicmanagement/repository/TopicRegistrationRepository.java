package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;

public interface TopicRegistrationRepository extends JpaRepository<TopicRegistration, Long> {
    Optional<TopicRegistration> findByStudentGroup_Id(Long groupId);
    boolean existsByTopic_Id(Long topicId);
    List<TopicRegistration> findByStatusOrderByRegisteredAtAsc(RegistrationStatus status);
    List<TopicRegistration> findByStudentGroup_GroupLeader_UsernameOrderByRegisteredAtDesc(String username);

    @Query("select tr from TopicRegistration tr join fetch tr.studentGroup g join fetch tr.topic t "
         + "where tr.status = vn.edu.demo.topicmanagement.entity.RegistrationStatus.APPROVED "
         + "and exists (select ts.id from TopicSupervisor ts where ts.topic = t and ts.lecturer.username = :username) "
         + "order by tr.registeredAt desc")
    List<TopicRegistration> findApprovedBySupervisorUsername(@Param("username") String username);

    @Query("select tr from TopicRegistration tr join fetch tr.studentGroup g join fetch g.groupLeader "
         + "join fetch tr.topic where tr.status = :status order by tr.registeredAt")
    List<TopicRegistration> findAllWithDetailsByStatus(@Param("status") RegistrationStatus status);

    @Query("select tr from TopicRegistration tr join fetch tr.studentGroup "
         + "join fetch tr.topic t join fetch t.registrationPeriod "
         + "where tr.status = :status and t.registrationPeriod.id = :periodId order by tr.registeredAt")
    List<TopicRegistration> findByPeriodAndStatusWithDetails(@Param("periodId") Long periodId,
                                                              @Param("status") RegistrationStatus status);

    @Query("select tr from TopicRegistration tr join fetch tr.studentGroup g join fetch tr.topic "
         + "where exists (select gm.id from GroupMember gm where gm.studentGroup = g and gm.student.username = :username) "
         + "order by tr.registeredAt desc")
    List<TopicRegistration> findForStudent(@Param("username") String username);
}
