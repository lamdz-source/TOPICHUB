package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.TopicForm;
import vn.edu.demo.topicmanagement.dto.TopicView;
import vn.edu.demo.topicmanagement.entity.*;
import vn.edu.demo.topicmanagement.repository.*;

/**
 * Nghiệp vụ về đề tài:
 *  - Giảng viên đăng ký / sửa đề tài (kèm 1-2 GVHD)
 *  - Trưởng khoa duyệt, từ chối, công bố
 */
@Service
public class TopicService {

    // Dùng giờ Việt Nam cho mọi phép so sánh thời gian (server deploy có thể đặt múi giờ khác)
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final TopicRepository topicRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final TopicRegistrationRepository registrationRepository;

    public TopicService(TopicRepository topicRepository,
                        TopicSupervisorRepository supervisorRepository,
                        UserRepository userRepository,
                        DepartmentRepository departmentRepository,
                        RegistrationPeriodRepository periodRepository,
                        TopicRegistrationRepository registrationRepository) {
        this.topicRepository = topicRepository;
        this.supervisorRepository = supervisorRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.periodRepository = periodRepository;
        this.registrationRepository = registrationRepository;
    }

    // ==================================================================
    // ĐỌC DỮ LIỆU
    // ==================================================================

    @Transactional(readOnly = true)
    public List<Topic> getPublishedTopics() {
        return topicRepository.findByStatusWithDetails(TopicStatus.PUBLISHED);
    }

    /** Đề tài của giảng viên đang đăng nhập (do mình tạo hoặc mình là GVHD). */
    @Transactional(readOnly = true)
    public List<TopicView> getMyTopics(String username) {
        return toViews(topicRepository.findByLecturer(username), username);
    }

    /** Tất cả đề tài - cho Trưởng khoa. */
    @Transactional(readOnly = true)
    public List<TopicView> getAllForHead() {
        return toViews(topicRepository.findAllWithDetails(), null);
    }

    // Dữ liệu cho các ô chọn trên form
    @Transactional(readOnly = true)
    public List<Department> getDepartments() {
        return departmentRepository.findAllByOrderByNameAsc();
    }

    /** Chỉ các đợt đang OPEN và đang trong giai đoạn giảng viên đăng ký. */
    @Transactional(readOnly = true)
    public List<RegistrationPeriod> getOpenPeriods() {
        return periodRepository.findOpenForLecturers(PeriodStatus.OPEN, now());
    }

    /** Giảng viên có thể chọn làm GVHD thứ hai: đang hoạt động và khác chính mình. */
    @Transactional(readOnly = true)
    public List<User> getCoSupervisorCandidates(String username) {
        return userRepository.findByRoleAndStatusOrderByFullNameAsc(UserRole.LECTURER, UserStatus.ACTIVE)
                .stream()
                .filter(u -> !u.getUsername().equals(username))
                .collect(Collectors.toList());
    }

    // ==================================================================
    // QUY TẮC NGHIỆP VỤ KHI GIẢNG VIÊN ĐĂNG KÝ ĐỀ TÀI
    // ==================================================================

    public Map<String, String> checkRules(TopicForm f, String username) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Quy tắc 1: chỉ đăng ký trong đợt đang mở và đúng giai đoạn của giảng viên
        if (f.getPeriodId() != null) {
            RegistrationPeriod p = periodRepository.findById(f.getPeriodId()).orElse(null);
            LocalDateTime now = now();
            if (p == null) {
                errors.put("periodId", "Đợt đăng ký không tồn tại");
            } else if (p.getStatus() != PeriodStatus.OPEN
                    || now.isBefore(p.getLecturerStart()) || now.isAfter(p.getLecturerEnd())) {
                errors.put("periodId", "Đợt này hiện không mở cho giảng viên đăng ký đề tài");
            }
        }

        // Quy tắc 2: bộ môn phải tồn tại
        if (f.getDepartmentId() != null && !departmentRepository.existsById(f.getDepartmentId())) {
            errors.put("departmentId", "Bộ môn không tồn tại");
        }

        // Quy tắc 3: mỗi đề tài có 1 đến 2 GVHD.
        // Người tạo là GVHD chính (luôn có -> đủ "ít nhất 1"); GVHD thứ hai là tùy chọn (-> "tối đa 2").
        if (f.getCoSupervisorId() != null) {
            User co = userRepository.findById(f.getCoSupervisorId()).orElse(null);
            if (co == null || co.getRole() != UserRole.LECTURER || co.getStatus() != UserStatus.ACTIVE) {
                errors.put("coSupervisorId", "GVHD thứ hai phải là giảng viên đang hoạt động");
            } else if (co.getUsername().equals(username)) {
                errors.put("coSupervisorId", "GVHD thứ hai phải khác chính bạn");
            }
        }
        return errors;
    }

    // ==================================================================
    // GIẢNG VIÊN: TẠO / SỬA
    // ==================================================================

    @Transactional
    public void create(TopicForm f, String username) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng đang đăng nhập"));

        Topic t = new Topic();
        copyFormToTopic(f, t);
        t.setCreatedBy(me);
        t.setStatus(TopicStatus.PENDING);      // đề tài mới luôn chờ Trưởng khoa duyệt
        topicRepository.save(t);

        saveSupervisors(t, me, f.getCoSupervisorId());
    }

    /** Mở trang sửa: kiểm tra quyền sửa rồi đổ dữ liệu cũ ra form. */
    @Transactional(readOnly = true)
    public TopicForm getFormForEdit(Long id, String username) {
        Topic t = loadEditable(id, username);

        TopicForm f = new TopicForm();
        f.setTitle(t.getTitle());
        f.setDescription(t.getDescription());
        f.setDepartmentId(t.getDepartment().getId());
        f.setPeriodId(t.getRegistrationPeriod().getId());
        // GVHD thứ hai (nếu có) = GVHD không phải "chính"
        supervisorRepository.findByTopicIds(List.of(id)).stream()
                .filter(ts -> !ts.isPrimarySupervisor())
                .findFirst()
                .ifPresent(ts -> f.setCoSupervisorId(ts.getLecturer().getId()));
        return f;
    }

    @Transactional
    public void update(Long id, TopicForm f, String username) {
        Topic t = loadEditable(id, username);
        copyFormToTopic(f, t);
        t.setStatus(TopicStatus.PENDING);      // sửa xong thì gửi duyệt lại
        t.setRejectReason(null);
        topicRepository.save(t);

        // Ghi lại danh sách GVHD: xóa hết cũ rồi thêm mới
        supervisorRepository.deleteByTopic(id);
        saveSupervisors(t, t.getCreatedBy(), f.getCoSupervisorId());
    }

    /** Deletes only the current lecturer's pending/rejected topic when no student registration refers to it. */
    @Transactional
    public void deleteOwnUnsubmitted(Long id, String username) {
        Topic topic = getTopic(id);
        if (!topic.getCreatedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải người tạo đề tài này.");
        }
        if (topic.getStatus() != TopicStatus.PENDING && topic.getStatus() != TopicStatus.REJECTED) {
            throw new IllegalStateException("Chỉ xóa được đề tài đang chờ duyệt hoặc bị từ chối.");
        }
        if (registrationRepository.existsByTopic_Id(id)) {
            throw new IllegalStateException("Không thể xóa đề tài đã có đăng ký của sinh viên.");
        }

        // Xóa liên kết GVHD trước vì database không cascade xóa các bản ghi phụ thuộc.
        supervisorRepository.deleteByTopic(id);
        topicRepository.delete(topic);
        topicRepository.flush();
    }

    // ==================================================================
    // TRƯỞNG KHOA: DUYỆT / TỪ CHỐI / CÔNG BỐ
    // ==================================================================

    @Transactional
    public void approve(Long id) {
        Topic t = getTopic(id);
        requireStatus(t, TopicStatus.PENDING, "Chỉ duyệt được đề tài đang chờ duyệt.");
        t.setStatus(TopicStatus.APPROVED);
    }

    @Transactional
    public void reject(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        }
        Topic t = getTopic(id);
        requireStatus(t, TopicStatus.PENDING, "Chỉ từ chối được đề tài đang chờ duyệt.");
        t.setStatus(TopicStatus.REJECTED);
        t.setRejectReason(reason.trim());
    }

    @Transactional
    public void publish(Long id) {
        Topic t = getTopic(id);
        requireStatus(t, TopicStatus.APPROVED, "Chỉ công bố được đề tài đã duyệt.");
        t.setStatus(TopicStatus.PUBLISHED);
    }

    /** Công bố cùng lúc tất cả đề tài đã duyệt. Trả về số đề tài được công bố. */
    @Transactional
    public int publishAllApproved() {
        List<Topic> approved = topicRepository.findByStatus(TopicStatus.APPROVED);
        approved.forEach(t -> t.setStatus(TopicStatus.PUBLISHED));
        topicRepository.saveAll(approved);
        return approved.size();
    }

    // ==================================================================
    // HÀM PHỤ
    // ==================================================================

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }

    private Topic getTopic(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài"));
    }

    private void requireStatus(Topic t, TopicStatus expected, String message) {
        if (t.getStatus() != expected) throw new IllegalStateException(message);
    }

    /** Lấy đề tài để sửa: phải là người tạo và đề tài đang chờ duyệt hoặc bị từ chối. */
    private Topic loadEditable(Long id, String username) {
        Topic t = getTopic(id);
        if (!t.getCreatedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải người tạo đề tài này");
        }
        if (t.getStatus() != TopicStatus.PENDING && t.getStatus() != TopicStatus.REJECTED) {
            throw new IllegalStateException("Chỉ sửa được đề tài đang chờ duyệt hoặc bị từ chối.");
        }
        return t;
    }

    private void copyFormToTopic(TopicForm f, Topic t) {
        t.setTitle(f.getTitle().trim());
        t.setDescription(f.getDescription().trim());
        t.setDepartment(departmentRepository.getReferenceById(f.getDepartmentId()));
        t.setRegistrationPeriod(periodRepository.getReferenceById(f.getPeriodId()));
    }

    private void saveSupervisors(Topic t, User primary, Long coSupervisorId) {
        supervisorRepository.save(new TopicSupervisor(t, primary, true));
        if (coSupervisorId != null) {
            supervisorRepository.save(new TopicSupervisor(t, userRepository.getReferenceById(coSupervisorId), false));
        }
    }

    /** Ghép mỗi đề tài với tên các GVHD (lấy GVHD của tất cả đề tài chỉ bằng 1 câu truy vấn). */
    private List<TopicView> toViews(List<Topic> topics, String username) {
        if (topics.isEmpty()) return new ArrayList<>();

        // Build ids with a loop: this also avoids an Eclipse null-annotation warning
        // caused by converting the stream lambda to Function<Topic, Long>.
        List<Long> ids = new ArrayList<>();
        for (Topic topic : topics) {
            ids.add(topic.getId());
        }
        Map<Long, String> namesByTopic = supervisorRepository.findByTopicIds(ids).stream()
                .collect(Collectors.groupingBy(
                        ts -> ts.getTopic().getId(),
                        Collectors.mapping(ts -> ts.getLecturer().getFullName(), Collectors.joining(", "))));

        List<TopicView> views = new ArrayList<>();
        for (Topic t : topics) {
            boolean editable = username != null
                    && t.getCreatedBy().getUsername().equals(username)
                    && (t.getStatus() == TopicStatus.PENDING || t.getStatus() == TopicStatus.REJECTED);
            views.add(new TopicView(t, namesByTopic.getOrDefault(t.getId(), ""), editable));
        }
        return views;
    }
}
