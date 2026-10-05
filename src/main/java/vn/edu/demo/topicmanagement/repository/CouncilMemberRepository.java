package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.CouncilMember;
import vn.edu.demo.topicmanagement.entity.CouncilRole;

public interface CouncilMemberRepository extends JpaRepository<CouncilMember, Long> {
    List<CouncilMember> findByCouncilId(Long councilId);
    List<CouncilMember> findByLecturerUsername(String username);
    long countByCouncilId(Long councilId);
    long countByCouncilIdAndRole(Long councilId, CouncilRole role);
    boolean existsByCouncilIdAndLecturerId(Long councilId, Long lecturerId);
}
