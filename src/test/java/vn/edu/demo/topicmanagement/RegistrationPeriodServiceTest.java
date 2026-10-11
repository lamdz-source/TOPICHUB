package vn.edu.demo.topicmanagement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.PeriodType;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;
import vn.edu.demo.topicmanagement.service.RegistrationPeriodService;

/** Unit tests of safely reopening registration periods; no DB connection needed. */
class RegistrationPeriodServiceTest {
    private RegistrationPeriodRepository periodRepository;
    private CouncilRepository councilRepository;
    private RegistrationPeriodService service;
    private RegistrationPeriod period;

    @BeforeEach
    void setUp() {
        periodRepository = mock(RegistrationPeriodRepository.class);
        councilRepository = mock(CouncilRepository.class);
        service = new RegistrationPeriodService(periodRepository, mock(TopicRepository.class),
                councilRepository, mock(UserRepository.class));
        period = new RegistrationPeriod();
        period.setId(20L);
        period.setStatus(PeriodStatus.CLOSED);
        period.setType(PeriodType.COURSE);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        period.setLecturerStart(now.minusDays(2));
        period.setLecturerEnd(now.plusDays(1));
        period.setStudentStart(now.plusDays(1));
        period.setStudentEnd(now.plusDays(8));
        when(periodRepository.findById(20L)).thenReturn(Optional.of(period));
    }

    @Test
    void reopensValidClosedPeriod() {
        assertDoesNotThrow(() -> service.reopen(20L));
        assertEquals(PeriodStatus.OPEN, period.getStatus());
    }

    @Test
    void rejectsExpiredStudentPhase() {
        period.setLecturerStart(LocalDateTime.now().minusDays(8));
        period.setLecturerEnd(LocalDateTime.now().minusDays(6));
        period.setStudentStart(LocalDateTime.now().minusDays(5));
        period.setStudentEnd(LocalDateTime.now().minusDays(1));
        assertThrows(IllegalStateException.class, () -> service.reopen(20L));
        assertEquals(PeriodStatus.CLOSED, period.getStatus());
    }

    @Test
    void rejectsPeriodWithCouncil() {
        when(councilRepository.existsByRegistrationPeriod_Id(20L)).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.reopen(20L));
        assertEquals(PeriodStatus.CLOSED, period.getStatus());
    }

    @Test
    void rejectsOpenOrDraftPeriod() {
        period.setStatus(PeriodStatus.DRAFT);
        assertThrows(IllegalStateException.class, () -> service.reopen(20L));
        assertEquals(PeriodStatus.DRAFT, period.getStatus());
    }
}
