package vn.edu.demo.topicmanagement.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.demo.topicmanagement.entity.Council;

public interface CouncilRepository extends JpaRepository<Council, Long> {
    List<Council> findByRegistrationPeriodIdOrderByDefenseDateAsc(Long periodId);
    boolean existsByRegistrationPeriod_Id(Long registrationPeriodId);
}
