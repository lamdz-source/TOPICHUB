package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.*;
import vn.edu.demo.topicmanagement.entity.*;
import vn.edu.demo.topicmanagement.repository.*;

/** Nghiệp vụ HEAD lập hội đồng, thêm thành viên/đề tài và kích hoạt hội đồng. */
@Service
public class CouncilService {
    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final TopicReviewerRepository reviewerRepository;
    private final ReportRepository reportRepository;
    private final RegistrationPeriodRepository periodRepository;
    private final UserRepository userRepository;

    public CouncilService(CouncilRepository councilRepository, CouncilMemberRepository memberRepository,
                          CouncilTopicRepository councilTopicRepository,
                          TopicRegistrationRepository registrationRepository,
                          TopicReviewerRepository reviewerRepository, ReportRepository reportRepository,
                          RegistrationPeriodRepository periodRepository, UserRepository userRepository) {
        this.councilRepository = councilRepository; this.memberRepository = memberRepository;
        this.councilTopicRepository = councilTopicRepository; this.registrationRepository = registrationRepository;
        this.reviewerRepository = reviewerRepository; this.reportRepository = reportRepository;
        this.periodRepository = periodRepository; this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RegistrationPeriod> getPeriodsForCouncil() {
        return periodRepository.findAll().stream()
                .filter(p -> p.getType() == PeriodType.TLCN || p.getType() == PeriodType.KLTN).toList();
    }

    @Transactional(readOnly = true)
    public List<CouncilSummaryView> getCouncils() {
        return councilRepository.findAll().stream().map(this::toSummary).toList();
    }

    @Transactional
    public Long create(CouncilForm form) {
        RegistrationPeriod period = periodRepository.findById(form.getPeriodId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đợt đăng ký."));
        if (period.getType() != PeriodType.TLCN && period.getType() != PeriodType.KLTN) {
            throw new IllegalArgumentException("Chỉ tạo hội đồng cho đợt TLCN/KLTN.");
        }
        LocalDateTime defenseDate = form.getDefenseDate() == null ? period.getDefenseDate() : form.getDefenseDate();
        if (defenseDate == null) throw new IllegalArgumentException("Hội đồng cần có ngày báo cáo.");
        if (period.getStudentEnd() != null && !defenseDate.isAfter(period.getStudentEnd())) {
            throw new IllegalArgumentException("Ngày báo cáo hội đồng phải sau khi kết thúc đăng ký của sinh viên.");
        }
        if (period.getReviewDeadline() != null && !defenseDate.isAfter(period.getReviewDeadline())) {
            throw new IllegalArgumentException("Ngày báo cáo hội đồng phải sau hạn nộp điểm phản biện.");
        }
        Council council = new Council();
        council.setName(form.getName().trim());
        council.setRegistrationPeriod(period);
        council.setDefenseDate(defenseDate);
        council.setRoom(form.getRoom() == null || form.getRoom().isBlank() ? null : form.getRoom().trim());
        council.setStatus(CouncilStatus.DRAFT);
        return councilRepository.save(council).getId();
    }

    @Transactional(readOnly = true)
    public CouncilDetailView getDetail(Long councilId) {
        Council council = getCouncil(councilId);
        List<CouncilMember> members = memberRepository.findByCouncilId(councilId);
        List<LecturerOption> lecturers = userRepository
                .findByRoleAndStatusOrderByFullNameAsc(UserRole.LECTURER, UserStatus.ACTIVE).stream()
                .map(u -> new LecturerOption(u.getId(), u.getFullName(), u.getLecturerCode())).toList();
        List<TopicRegistration> periodRegistrations = registrationRepository
                .findByPeriodAndStatusWithDetails(council.getRegistrationPeriod().getId(), RegistrationStatus.APPROVED);
        Set<Long> assignedIds = councilTopicRepository.findByCouncilId(councilId).stream()
                .map(ct -> ct.getTopicRegistration().getId()).collect(Collectors.toSet());

        List<CouncilRegistrationOption> assigned = new java.util.ArrayList<>();
        List<CouncilRegistrationOption> available = new java.util.ArrayList<>();
        for (TopicRegistration registration : periodRegistrations) {
            CouncilRegistrationOption option = toRegistrationOption(registration);
            if (assignedIds.contains(registration.getId())) assigned.add(option);
            else if (hasReportAndCompletedReviews(registration)) available.add(option);
        }
        List<CouncilMemberView> memberViews = members.stream().map(m -> new CouncilMemberView(
                m.getId(), m.getLecturer().getFullName(), m.getLecturer().getLecturerCode(), m.getRole())).toList();
        return new CouncilDetailView(toSummary(council), memberViews, lecturers, available, assigned);
    }

    @Transactional
    public void addMember(Long councilId, CouncilMemberForm form) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        long count = memberRepository.countByCouncilId(councilId);
        if (count >= 5) throw new IllegalStateException("Một hội đồng có tối đa 5 giảng viên.");
        User lecturer = userRepository.findById(form.getLecturerId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giảng viên."));
        if (lecturer.getRole() != UserRole.LECTURER || lecturer.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Thành viên hội đồng phải là giảng viên đang hoạt động.");
        }
        if (memberRepository.existsByCouncilIdAndLecturerId(councilId, lecturer.getId())) {
            throw new IllegalStateException("Giảng viên này đã ở trong hội đồng.");
        }
        if (form.getRole() == CouncilRole.CHAIRMAN
                && memberRepository.countByCouncilIdAndRole(councilId, CouncilRole.CHAIRMAN) > 0) {
            throw new IllegalStateException("Hội đồng chỉ có một chủ tịch.");
        }
        if (form.getRole() == CouncilRole.SECRETARY
                && memberRepository.countByCouncilIdAndRole(councilId, CouncilRole.SECRETARY) > 0) {
            throw new IllegalStateException("Hội đồng chỉ có một thư ký.");
        }
        memberRepository.save(new CouncilMember(council, lecturer, form.getRole()));
    }

    @Transactional
    public void removeMember(Long councilId, Long memberId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        CouncilMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên hội đồng."));
        if (!member.getCouncil().getId().equals(councilId)) {
            throw new IllegalArgumentException("Thành viên này không thuộc hội đồng đã chọn.");
        }
        memberRepository.delete(member);
    }

    @Transactional
    public void addTopic(Long councilId, Long registrationId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        TopicRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        if (registration.getStatus() != RegistrationStatus.APPROVED
                || !registration.getTopic().getRegistrationPeriod().getId().equals(council.getRegistrationPeriod().getId())) {
            throw new IllegalArgumentException("Đề tài phải được duyệt và thuộc cùng đợt với hội đồng.");
        }
        if (councilTopicRepository.findByTopicRegistrationId(registrationId).isPresent()) {
            throw new IllegalStateException("Đề tài này đã được xếp vào một hội đồng.");
        }
        if (!hasReportAndCompletedReviews(registration)) {
            throw new IllegalStateException("Cần có báo cáo đã nộp và đủ điểm GVPB trước khi xếp hội đồng.");
        }
        councilTopicRepository.save(new CouncilTopic(council, registration));
    }

    @Transactional
    public void removeTopic(Long councilId, Long registrationId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        CouncilTopic councilTopic = councilTopicRepository.findByTopicRegistrationId(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Đề tài chưa được xếp vào hội đồng."));
        if (!councilTopic.getCouncil().getId().equals(councilId)) {
            throw new IllegalArgumentException("Đề tài này không thuộc hội đồng đã chọn.");
        }
        councilTopicRepository.delete(councilTopic);
    }

    @Transactional
    public void activate(Long councilId) {
        Council council = getCouncil(councilId);
        requireDraft(council);
        long memberCount = memberRepository.countByCouncilId(councilId);
        if (memberCount < 3 || memberCount > 5) throw new IllegalStateException("Hội đồng phải có từ 3 đến 5 giảng viên.");
        if (memberRepository.countByCouncilIdAndRole(councilId, CouncilRole.CHAIRMAN) != 1
                || memberRepository.countByCouncilIdAndRole(councilId, CouncilRole.SECRETARY) != 1) {
            throw new IllegalStateException("Hội đồng phải có đúng một chủ tịch và một thư ký.");
        }
        if (councilTopicRepository.findByCouncilId(councilId).isEmpty()) {
            throw new IllegalStateException("Hãy xếp ít nhất một đề tài vào hội đồng.");
        }
        council.setStatus(CouncilStatus.ACTIVE);
    }

    private boolean hasReportAndCompletedReviews(TopicRegistration registration) {
        if (reportRepository.findByTopicRegistrationId(registration.getId()).isEmpty()) return false;
        List<TopicReviewer> reviewers = reviewerRepository.findByTopicRegistrationId(registration.getId());
        return !reviewers.isEmpty() && reviewers.stream().allMatch(r -> r.getReviewScore() != null);
    }

    private CouncilSummaryView toSummary(Council council) {
        List<String> memberNames = memberRepository.findByCouncilId(council.getId()).stream()
                .map(m -> m.getRole().getDisplayName() + ": " + m.getLecturer().getFullName()).toList();
        List<String> topicNames = councilTopicRepository.findByCouncilId(council.getId()).stream()
                .map(ct -> ct.getTopicRegistration().getTopic().getTitle()).toList();
        return new CouncilSummaryView(council.getId(), council.getName(), council.getRegistrationPeriod().getName(),
                council.getDefenseDate(), council.getRoom(), council.getStatus(), memberNames, topicNames);
    }

    private CouncilRegistrationOption toRegistrationOption(TopicRegistration registration) {
        return new CouncilRegistrationOption(registration.getId(), registration.getStudentGroup().getName(),
                registration.getTopic().getTitle());
    }

    private Council getCouncil(Long id) {
        return councilRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hội đồng."));
    }

    private void requireDraft(Council council) {
        if (council.getStatus() != CouncilStatus.DRAFT) throw new IllegalStateException("Chỉ chỉnh sửa hội đồng đang ở trạng thái nháp.");
    }
}
