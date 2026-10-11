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
import vn.edu.demo.topicmanagement.dto.HeadSupervisorForm;
import vn.edu.demo.topicmanagement.dto.HeadSupervisorInfo;
import vn.edu.demo.topicmanagement.dto.LecturerOption;
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
    private final TopicReviewerRepository reviewerRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final ResultRepository resultRepository;

    public TopicService(TopicRepository topicRepository,
                        TopicSupervisorRepository supervisorRepository,
                        UserRepository userRepository,
                        DepartmentRepository departmentRepository,
                        RegistrationPeriodRepository periodRepository,
                        TopicRegistrationRepository registrationRepository,
                        TopicReviewerRepository reviewerRepository,
                        CouncilTopicRepository councilTopicRepository,
                        CouncilMemberRepository councilMemberRepository,
                        ResultRepository resultRepository) {
        this.topicRepository = topicRepository;
        this.supervisorRepository = supervisorRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.periodRepository = periodRepository;
        this.registrationRepository = registrationRepository;
        this.reviewerRepository = reviewerRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.resultRepository = resultRepository;
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
        requireCreatorIsPrimarySupervisor(topic);

        // Xóa liên kết GVHD trước vì database không cascade xóa các bản ghi phụ thuộc.
        supervisorRepository.deleteByTopic(id);
        topicRepository.delete(topic);
        topicRepository.flush();
    }

    // ==================================================================
    // TRƯỞNG KHOA: PHÂN CÔNG / THAY ĐỔI GIẢNG VIÊN HƯỚNG DẪN
    // ==================================================================

    @Transactional(readOnly = true)
    public List<LecturerOption> getActiveLecturerOptions() {
        return userRepository.findByRoleAndStatusOrderByFullNameAsc(UserRole.LECTURER, UserStatus.ACTIVE)
                .stream()
                .map(u -> new LecturerOption(u.getId(), u.getFullName(), u.getLecturerCode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public HeadSupervisorInfo getHeadSupervisorInfo(Long topicId) {
        Topic topic = getTopic(topicId);
        List<TopicSupervisor> assigned = supervisorRepository.findByTopicIds(List.of(topicId));
        Long main = null;
        Long secondary = null;
        List<String> names = new ArrayList<>();
        for (TopicSupervisor entry : assigned) {
            names.add(entry.getLecturer().getFullName() + (entry.isPrimarySupervisor() ? " (chính)" : " (thứ hai)"));
            if (entry.isPrimarySupervisor()) main = entry.getLecturer().getId();
            else secondary = entry.getLecturer().getId();
        }
        return new HeadSupervisorInfo(topic.getTitle(), topic.getRegistrationPeriod().getName(),
                topic.getStatus().getLabel(), String.join(", ", names), main, secondary);
    }

    /**
     * Thay danh sách GVHD trong một giao dịch: đúng 1 GVHD chính, tối đa 1 GVHD thứ hai.
     * Không cho thay đổi khi đã có kết quả hội đồng để tránh sửa lịch sử chấm điểm.
     * Không cho chỉ định người đang phản biện hoặc thuộc hội đồng chấm đề tài đó.
     */
    @Transactional
    public void updateSupervisorsByHead(Long topicId, HeadSupervisorForm form) {
        Topic topic = getTopic(topicId);
        if (topic.getStatus() == TopicStatus.REJECTED) {
            throw new IllegalStateException("Không phân công GVHD cho đề tài bị từ chối.");
        }
        if (form == null || form.getPrimarySupervisorId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn GVHD chính.");
        }
        Long primaryId = form.getPrimarySupervisorId();
        Long secondaryId = form.getCoSupervisorId();
        if (primaryId.equals(secondaryId)) {
            throw new IllegalArgumentException("GVHD chính và GVHD thứ hai phải khác nhau.");
        }

        // Kiểm tra quyền của GIẢNG VIÊN theo ID ở server, không tin giá trị gửi từ trình duyệt.
        validateActiveLecturer(primaryId);
        if (secondaryId != null) validateActiveLecturer(secondaryId);

        List<TopicSupervisor> old = supervisorRepository.findByTopicIds(List.of(topicId));
        if (old.size() == (secondaryId == null ? 1 : 2)
                && old.stream().anyMatch(ts -> ts.isPrimarySupervisor() && primaryId.equals(ts.getLecturer().getId()))
                && (secondaryId == null || old.stream()
                    .anyMatch(ts -> !ts.isPrimarySupervisor() && secondaryId.equals(ts.getLecturer().getId())))) {
            return; // Không thay đổi: không xóa rồi ghi lại dữ liệu một cách không cần thiết.
        }

        for (TopicRegistration registration : registrationRepository.findByTopic_Id(topicId)) {
            if (resultRepository.findByTopicRegistrationId(registration.getId()).isPresent()) {
                throw new IllegalStateException("Đề tài đã có kết quả hội đồng; không thể thay đổi GVHD.");
            }
            for (Long candidateId : secondaryId == null ? List.of(primaryId) : List.of(primaryId, secondaryId)) {
                if (reviewerRepository.existsByTopicRegistrationIdAndLecturerId(registration.getId(), candidateId)) {
                    throw new IllegalStateException("GVHD được chọn đang được phân công phản biện đề tài này.");
                }
                councilTopicRepository.findByTopicRegistrationId(registration.getId()).ifPresent(ct -> {
                    if (councilMemberRepository.existsByCouncilIdAndLecturerId(ct.getCouncil().getId(), candidateId)) {
                        throw new IllegalStateException("GVHD được chọn đang là thành viên hội đồng chấm đề tài này.");
                    }
                });
            }
        }

        // Thao tác xóa/thêm cùng một transaction: gặp lỗi thì Spring rollback.
        supervisorRepository.deleteByTopic(topicId);
        supervisorRepository.save(new TopicSupervisor(topic, userRepository.getReferenceById(primaryId), true));
        if (secondaryId != null) {
            supervisorRepository.save(new TopicSupervisor(topic, userRepository.getReferenceById(secondaryId), false));
        }
    }

    private void validateActiveLecturer(Long id) {
        User lecturer = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên đã chọn."));
        if (lecturer.getRole() != UserRole.LECTURER || lecturer.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Chỉ được phân công giảng viên đang hoạt động.");
        }
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
        // Giữ nguyên quyết định phân công của Trưởng khoa: giảng viên tạo đề tài
        // không được tự đặt mình lại làm GVHD chính sau khi Trưởng khoa đã thay đổi.
        requireCreatorIsPrimarySupervisor(t);
        return t;
    }

    private void requireCreatorIsPrimarySupervisor(Topic topic) {
        boolean creatorIsPrimary = supervisorRepository.findByTopicIds(List.of(topic.getId())).stream()
                .anyMatch(ts -> ts.isPrimarySupervisor()
                        && topic.getCreatedBy().getId().equals(ts.getLecturer().getId()));
        if (!creatorIsPrimary) {
            throw new IllegalStateException("GVHD chính đã được Trưởng khoa thay đổi; vui lòng liên hệ Trưởng khoa để chỉnh sửa đề tài.");
        }
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
        List<TopicSupervisor> supervisorAssignments = supervisorRepository.findByTopicIds(ids);
        Map<Long, String> namesByTopic = supervisorAssignments.stream()
                .collect(Collectors.groupingBy(
                        ts -> ts.getTopic().getId(),
                        Collectors.mapping(ts -> ts.getLecturer().getFullName(), Collectors.joining(", "))));
        Map<Long, Long> primaryByTopic = new LinkedHashMap<>();
        for (TopicSupervisor ts : supervisorAssignments) {
            if (ts.isPrimarySupervisor()) primaryByTopic.put(ts.getTopic().getId(), ts.getLecturer().getId());
        }

        List<TopicView> views = new ArrayList<>();
        for (Topic t : topics) {
            boolean editable = username != null
                    && t.getCreatedBy().getUsername().equals(username)
                    && t.getCreatedBy().getId().equals(primaryByTopic.get(t.getId()))
                    && (t.getStatus() == TopicStatus.PENDING || t.getStatus() == TopicStatus.REJECTED);
            views.add(new TopicView(t, namesByTopic.getOrDefault(t.getId(), ""), editable));
        }
        return views;
    }
}
