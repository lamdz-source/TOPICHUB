package vn.edu.demo.topicmanagement.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;

public interface RegistrationPeriodRepository extends JpaRepository<RegistrationPeriod, Long> {

    @Query("select p from RegistrationPeriod p join fetch p.createdBy order by p.id desc")
    List<RegistrationPeriod> findAllWithCreator();

    // Các đợt đang mở cho GIẢNG VIÊN đăng ký: trạng thái OPEN và thời điểm hiện tại nằm trong giai đoạn 1
    @Query("select p from RegistrationPeriod p where p.status = :status "
         + "and p.lecturerStart <= :now and p.lecturerEnd >= :now order by p.id desc")
    List<RegistrationPeriod> findOpenForLecturers(@Param("status") PeriodStatus status,
                                                  @Param("now") LocalDateTime now);
}
