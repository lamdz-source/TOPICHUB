package vn.edu.demo.topicmanagement.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.GroupStatus;
import vn.edu.demo.topicmanagement.entity.StudentGroup;

public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    Optional<StudentGroup> findByGroupLeaderUsername(String username);
    List<StudentGroup> findByStatusOrderByIdDesc(GroupStatus status);
}
