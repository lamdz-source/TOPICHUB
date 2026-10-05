package vn.edu.demo.topicmanagement.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.demo.topicmanagement.dto.RegistrationPeriodForm;
import vn.edu.demo.topicmanagement.entity.PeriodStatus;
import vn.edu.demo.topicmanagement.entity.PeriodType;
import vn.edu.demo.topicmanagement.entity.RegistrationPeriod;
import vn.edu.demo.topicmanagement.entity.User;
import vn.edu.demo.topicmanagement.repository.RegistrationPeriodRepository;
import vn.edu.demo.topicmanagement.repository.TopicRepository;
import vn.edu.demo.topicmanagement.repository.CouncilRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/** Nghiệp vụ quản lý các đợt đăng ký của Trưởng khoa. */
@Service
public class RegistrationPeriodService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final RegistrationPeriodRepository periodRepository;
    private final TopicRepository topicRepository;
    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;

    public RegistrationPeriodService(RegistrationPeriodRepository periodRepository,
                                     TopicRepository topicRepository,
                                     CouncilRepository councilRepository,
                                     UserRepository userRepository) {
        this.periodRepository = periodRepository;
        this.topicRepository = topicRepository;
        this.councilRepository = councilRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> findAll() {
        return periodRepository.findAllWithCreator();
    }

    public Map<String, String> checkRules(RegistrationPeriodForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (form.getLecturerStart() != null && form.getLecturerEnd() != null
                && !form.getLecturerEnd().isAfter(form.getLecturerStart())) {
            errors.put("lecturerEnd", "Thời gian kết thúc đăng ký của GV phải sau thời gian bắt đầu.");
        }
        if (form.getStudentStart() != null && form.getStudentEnd() != null
                && !form.getStudentEnd().isAfter(form.getStudentStart())) {
            errors.put("studentEnd", "Thời gian kết thúc đăng ký của SV phải sau thời gian bắt đầu.");
        }
        if (form.getLecturerEnd() != null && form.getStudentStart() != null
                && form.getStudentStart().isBefore(form.getLecturerEnd())) {
            errors.put("studentStart", "Giai đoạn SV phải bắt đầu sau khi giai đoạn GV kết thúc.");
        }
        if (form.getStudentEnd() != null && form.getReviewDeadline() != null
                && !form.getReviewDeadline().isAfter(form.getStudentEnd())) {
            errors.put("reviewDeadline", "Hạn nộp điểm phản biện phải sau khi kết thúc đăng ký của sinh viên.");
        }
        if (form.getType() != null) {
            boolean thesisType = form.getType() == PeriodType.TLCN || form.getType() == PeriodType.KLTN;
            if (thesisType && form.getReviewDeadline() == null) {
                errors.put("reviewDeadline", "Đợt TLCN/KLTN bắt buộc có hạn GVPB nộp điểm.");
            }
            if (!thesisType && form.getReviewDeadline() != null) {
                errors.put("reviewDeadline", "Chỉ đợt TLCN/KLTN mới có hạn nộp điểm phản biện.");
            }
            if (form.getType() == PeriodType.KLTN && form.getDefenseDate() == null) {
                errors.put("defenseDate", "Đợt KLTN bắt buộc có ngày báo cáo hội đồng.");
            }
            if (form.getType() == PeriodType.KLTN && form.getDefenseDate() != null
                    && form.getReviewDeadline() != null
                    && !form.getDefenseDate().isAfter(form.getReviewDeadline())) {
                errors.put("defenseDate", "Ngày báo cáo hội đồng phải sau hạn nộp điểm phản biện.");
            }
            if (form.getType() == PeriodType.KLTN && form.getDefenseDate() != null
                    && form.getStudentEnd() != null
                    && !form.getDefenseDate().isAfter(form.getStudentEnd())) {
                errors.put("defenseDate", "Ngày báo cáo hội đồng phải sau khi kết thúc đăng ký của sinh viên.");
            }
            if (form.getType() != PeriodType.KLTN && form.getDefenseDate() != null) {
                errors.put("defenseDate", "Chỉ đợt KLTN mới có ngày báo cáo hội đồng.");
            }
        }
        return errors;
    }

    @Transactional
    public void create(RegistrationPeriodForm form, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản đang đăng nhập."));
        RegistrationPeriod period = new RegistrationPeriod();
        period.setName(form.getName().trim());
        period.setType(form.getType());
        period.setLecturerStart(form.getLecturerStart());
        period.setLecturerEnd(form.getLecturerEnd());
        period.setStudentStart(form.getStudentStart());
        period.setStudentEnd(form.getStudentEnd());
        period.setReviewDeadline(form.getReviewDeadline());
        period.setDefenseDate(form.getDefenseDate());
        period.setStatus(PeriodStatus.DRAFT);
        period.setCreatedBy(creator);
        periodRepository.save(period);
    }

    @Transactional(readOnly = true)
    public RegistrationPeriodForm getFormForEdit(Long id) {
        RegistrationPeriod period = getById(id);
        requireDraft(period);
        RegistrationPeriodForm form = new RegistrationPeriodForm();
        form.setName(period.getName());
        form.setType(period.getType());
        form.setLecturerStart(period.getLecturerStart());
        form.setLecturerEnd(period.getLecturerEnd());
        form.setStudentStart(period.getStudentStart());
        form.setStudentEnd(period.getStudentEnd());
        form.setReviewDeadline(period.getReviewDeadline());
        form.setDefenseDate(period.getDefenseDate());
        return form;
    }

    @Transactional
    public void update(Long id, RegistrationPeriodForm form) {
        RegistrationPeriod period = getById(id);
        requireDraft(period);
        period.setName(form.getName().trim());
        period.setType(form.getType());
        period.setLecturerStart(form.getLecturerStart());
        period.setLecturerEnd(form.getLecturerEnd());
        period.setStudentStart(form.getStudentStart());
        period.setStudentEnd(form.getStudentEnd());
        period.setReviewDeadline(form.getReviewDeadline());
        period.setDefenseDate(form.getDefenseDate());
        periodRepository.save(period);
    }

    @Transactional
    public void deleteDraft(Long id) {
        RegistrationPeriod period = getById(id);
        requireDraft(period);
        if (topicRepository.existsByRegistrationPeriod_Id(id)
                || councilRepository.existsByRegistrationPeriod_Id(id)) {
            throw new IllegalStateException("Không thể xóa đợt đã có đề tài hoặc hội đồng liên kết.");
        }
        periodRepository.delete(period);
    }

    @Transactional
    public void open(Long id) {
        RegistrationPeriod period = getById(id);
        if (period.getStatus() != PeriodStatus.DRAFT) {
            throw new IllegalStateException("Chỉ mở được đợt đang ở trạng thái nháp.");
        }
        Map<String, String> errors = checkRules(toForm(period));
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Không thể mở đợt: " + errors.values().iterator().next());
        }
        LocalDateTime currentTime = LocalDateTime.now(ZONE);
        boolean lecturerPhaseEnded = period.getLecturerEnd() != null
                && !period.getLecturerEnd().isAfter(currentTime);
        boolean studentPhaseEnded = period.getStudentEnd() != null
                && !period.getStudentEnd().isAfter(currentTime);
        if (lecturerPhaseEnded && studentPhaseEnded) {
            throw new IllegalStateException("Không thể mở đợt vì giai đoạn đăng ký của GV và SV đều đã kết thúc. Hãy tạo đợt mới với lịch phù hợp.");
        }
        period.setStatus(PeriodStatus.OPEN);
    }

    @Transactional
    public void close(Long id) {
        RegistrationPeriod period = getById(id);
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new IllegalStateException("Chỉ đóng được đợt đang mở.");
        }
        period.setStatus(PeriodStatus.CLOSED);
    }

    private RegistrationPeriod getById(Long id) {
        return periodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký."));
    }

    private RegistrationPeriodForm toForm(RegistrationPeriod period) {
        RegistrationPeriodForm form = new RegistrationPeriodForm();
        form.setName(period.getName());
        form.setType(period.getType());
        form.setLecturerStart(period.getLecturerStart());
        form.setLecturerEnd(period.getLecturerEnd());
        form.setStudentStart(period.getStudentStart());
        form.setStudentEnd(period.getStudentEnd());
        form.setReviewDeadline(period.getReviewDeadline());
        form.setDefenseDate(period.getDefenseDate());
        return form;
    }

    private void requireDraft(RegistrationPeriod period) {
        if (period.getStatus() != PeriodStatus.DRAFT) {
            throw new IllegalStateException("Chỉ chỉnh sửa được đợt đăng ký đang ở trạng thái nháp.");
        }
    }
}
