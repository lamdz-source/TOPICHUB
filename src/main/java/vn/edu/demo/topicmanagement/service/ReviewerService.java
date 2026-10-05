package vn.edu.demo.topicmanagement.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.LecturerOption;
import vn.edu.demo.topicmanagement.dto.ReviewAssignmentView;
import vn.edu.demo.topicmanagement.dto.ReviewAssigneeView;
import vn.edu.demo.topicmanagement.dto.ReviewerTaskView;
import vn.edu.demo.topicmanagement.dto.ReviewScoreForm;
import vn.edu.demo.topicmanagement.entity.*;
import vn.edu.demo.topicmanagement.repository.*;

/** Quản lý phân công GVPB và nhận điểm/nhận xét trước hạn của từng đợt. */
@Service
public class ReviewerService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final TopicRegistrationRepository registrationRepository;
    private final TopicReviewerRepository reviewerRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final UserRepository userRepository;
    private final StudentReportService reportService;

    public ReviewerService(TopicRegistrationRepository registrationRepository,
                           TopicReviewerRepository reviewerRepository,
                           TopicSupervisorRepository supervisorRepository,
                           UserRepository userRepository,
                           StudentReportService reportService) {
        this.registrationRepository = registrationRepository;
        this.reviewerRepository = reviewerRepository;
        this.supervisorRepository = supervisorRepository;
        this.userRepository = userRepository;
        this.reportService = reportService;
    }

    /** Trưởng khoa chỉ phân công đề tài TLCN/KLTN đã được duyệt. */
    @Transactional(readOnly = true)
    public List<ReviewAssignmentView> getAssignmentsForHead() {
        List<TopicRegistration> registrations = registrationRepository
                .findAllWithDetailsByStatus(RegistrationStatus.APPROVED).stream()
                .filter(this::hasReviewDeadline)
                .toList();
        if (registrations.isEmpty()) return List.of();

        List<Long> topicIds = registrations.stream().map(r -> r.getTopic().getId()).distinct().toList();
        Map<Long, Set<Long>> supervisorIds = supervisorRepository.findByTopicIds(topicIds).stream()
                .collect(Collectors.groupingBy(ts -> ts.getTopic().getId(),
                        Collectors.mapping(ts -> ts.getLecturer().getId(), Collectors.toSet())));
        List<LecturerOption> lecturers = userRepository
                .findByRoleAndStatusOrderByFullNameAsc(UserRole.LECTURER, UserStatus.ACTIVE).stream()
                .map(user -> new LecturerOption(user.getId(), user.getFullName(), user.getLecturerCode())).toList();

        List<ReviewAssignmentView> rows = new ArrayList<>();
        for (TopicRegistration registration : registrations) {
            List<TopicReviewer> assigned = reviewerRepository.findByTopicRegistrationId(registration.getId());
            Set<Long> excluded = new HashSet<>(supervisorIds.getOrDefault(registration.getTopic().getId(), Set.of()));
            assigned.forEach(row -> excluded.add(row.getLecturer().getId()));
            List<LecturerOption> candidates = lecturers.stream().filter(l -> !excluded.contains(l.getId())).toList();
            List<ReviewAssigneeView> assignees = assigned.stream()
                    .map(row -> new ReviewAssigneeView(row.getId(), row.getLecturer().getFullName(),
                            row.getReviewScore() != null))
                    .toList();
            rows.add(new ReviewAssignmentView(registration.getId(), registration.getStudentGroup().getName(),
                    registration.getTopic().getTitle(), registration.getTopic().getRegistrationPeriod().getType().getDisplayName(),
                    registration.getTopic().getRegistrationPeriod().getReviewDeadline(),
                    assigned.stream().map(r -> r.getLecturer().getFullName()).toList(), assignees, candidates));
        }
        return rows;
    }

    /** Gán một giảng viên đang hoạt động, không phải GVHD, cho đăng ký đã duyệt. */
    @Transactional
    public void assign(Long registrationId, Long lecturerId) {
        TopicRegistration registration = getRegistration(registrationId);
        if (registration.getStatus() != RegistrationStatus.APPROVED || !hasReviewDeadline(registration)) {
            throw new IllegalStateException("Chỉ phân công phản biện cho đăng ký TLCN/KLTN đã được duyệt.");
        }
        LocalDateTime deadline = registration.getTopic().getRegistrationPeriod().getReviewDeadline();
        if (deadline == null || now().isAfter(deadline)) {
            throw new IllegalStateException("Đã quá hạn phân công/nộp điểm phản biện.");
        }
        User lecturer = userRepository.findById(lecturerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên."));
        if (lecturer.getRole() != UserRole.LECTURER || lecturer.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Người phản biện phải là giảng viên đang hoạt động.");
        }
        boolean isSupervisor = supervisorRepository.findByTopicIds(List.of(registration.getTopic().getId())).stream()
                .anyMatch(ts -> ts.getLecturer().getId().equals(lecturerId));
        if (isSupervisor) throw new IllegalArgumentException("Không thể phân công GVHD làm phản biện cho đề tài của mình.");
        if (reviewerRepository.existsByTopicRegistrationIdAndLecturerId(registrationId, lecturerId)) {
            throw new IllegalStateException("Giảng viên này đã được phân công cho đề tài.");
        }
        reviewerRepository.save(new TopicReviewer(registration, lecturer));
    }

    @Transactional
    public void removeAssignment(Long registrationId, Long assignmentId) {
        TopicRegistration registration = getRegistration(registrationId);
        if (registration.getStatus() != RegistrationStatus.APPROVED || !hasReviewDeadline(registration)) {
            throw new IllegalStateException("Chỉ chỉnh sửa phân công phản biện cho đề tài TLCN/KLTN đã duyệt.");
        }
        LocalDateTime deadline = registration.getTopic().getRegistrationPeriod().getReviewDeadline();
        if (deadline == null || now().isAfter(deadline)) {
            throw new IllegalStateException("Đã quá hạn chỉnh sửa phân công phản biện.");
        }
        TopicReviewer assignment = reviewerRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công phản biện."));
        if (!assignment.getTopicRegistration().getId().equals(registrationId)) {
            throw new IllegalArgumentException("Phân công này không thuộc đề tài đã chọn.");
        }
        if (assignment.getReviewScore() != null || assignment.getReviewSubmittedAt() != null) {
            throw new IllegalStateException("Không thể gỡ phản biện đã nộp điểm.");
        }
        reviewerRepository.delete(assignment);
    }

    /** Danh sách đề tài được giao cho giảng viên hiện tại. */
    @Transactional(readOnly = true)
    public List<ReviewerTaskView> getMyTasks(String username) {
        return reviewerRepository.findByLecturerUsernameOrderByAssignedAtDesc(username).stream()
                .map(row -> {
                    LocalDateTime deadline = row.getTopicRegistration().getTopic().getRegistrationPeriod().getReviewDeadline();
                    return new ReviewerTaskView(row.getId(), row.getTopicRegistration().getId(),
                            row.getTopicRegistration().getStudentGroup().getName(),
                            row.getTopicRegistration().getTopic().getTitle(), deadline,
                            row.getReviewScore(), row.getReviewComment(), row.getReviewSubmittedAt(),
                            deadline != null && !now().isAfter(deadline),
                            reportService.isDownloadAvailable(row.getTopicRegistration().getId()));
                })
                .toList();
    }

    /** Lưu điểm trong khoảng 0–10 trước hoặc đúng hạn; GVPB có thể cập nhật tới hạn. */
    @Transactional
    public void submitReview(Long assignmentId, String username, ReviewScoreForm form) {
        TopicReviewer assignment = reviewerRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy phân công phản biện."));
        if (!assignment.getLecturer().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Đây không phải phân công của bạn.");
        }
        LocalDateTime deadline = assignment.getTopicRegistration().getTopic().getRegistrationPeriod().getReviewDeadline();
        if (deadline == null || now().isAfter(deadline)) {
            throw new IllegalStateException("Đã quá hạn nộp điểm phản biện.");
        }
        BigDecimal score = form.getScore();
        if (score == null || score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10.");
        }
        assignment.setReviewScore(score.setScale(2, java.math.RoundingMode.HALF_UP));
        assignment.setReviewComment(form.getComment() == null ? null : form.getComment().trim());
        assignment.setReviewSubmittedAt(now());
        reviewerRepository.save(assignment);
    }

    private TopicRegistration getRegistration(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
    }

    private boolean hasReviewDeadline(TopicRegistration registration) {
        PeriodType type = registration.getTopic().getRegistrationPeriod().getType();
        return (type == PeriodType.TLCN || type == PeriodType.KLTN)
                && registration.getTopic().getRegistrationPeriod().getReviewDeadline() != null;
    }

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
}
