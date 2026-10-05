package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByStudentId(Long studentId);
    Optional<GroupMember> findByStudentUsername(String username);
    List<GroupMember> findByStudentGroup_Id(Long groupId);
    long countByStudentGroup_Id(Long groupId);
}
