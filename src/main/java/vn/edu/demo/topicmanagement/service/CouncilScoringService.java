package vn.edu.demo.topicmanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.CouncilResultForm;
import vn.edu.demo.topicmanagement.dto.CouncilWorkView;
import vn.edu.demo.topicmanagement.dto.HeadResultView;
import vn.edu.demo.topicmanagement.dto.RegistrationScoreSummary;
import vn.edu.demo.topicmanagement.dto.ReviewScoreForm;
import vn.edu.demo.topicmanagement.dto.StudentResultView;
import vn.edu.demo.topicmanagement.entity.*;
import vn.edu.demo.topicmanagement.repository.*;

/** Giảng viên chấm điểm; chủ tịch tổng hợp, HEAD công bố kết quả. */
@Service
public class CouncilScoringService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final CouncilTopicRepository councilTopicRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final ScoreRepository scoreRepository;
    private final ResultRepository resultRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;
    private final StudentReportService reportService;

    public CouncilScoringService(CouncilTopicRepository councilTopicRepository,
                                 CouncilMemberRepository councilMemberRepository,
                                 TopicSupervisorRepository supervisorRepository,
                                 ScoreRepository scoreRepository, ResultRepository resultRepository,
                                 GroupMemberRepository groupMemberRepository,
                                 TopicRegistrationRepository registrationRepository,
                                 CouncilRepository councilRepository, UserRepository userRepository,
                                 StudentReportService reportService) {
        this.councilTopicRepository = councilTopicRepository; this.councilMemberRepository = councilMemberRepository;
        this.supervisorRepository = supervisorRepository; this.scoreRepository = scoreRepository;
        this.resultRepository = resultRepository; this.groupMemberRepository = groupMemberRepository;
        this.registrationRepository = registrationRepository; this.councilRepository = councilRepository;
        this.userRepository = userRepository;
        this.reportService = reportService;
    }

    /** Chỉ trả các đề tài thuộc hội đồng đang hoạt động mà giảng viên là thành viên. */
    @Transactional(readOnly = true)
    public List<CouncilWorkView> getMyWork(String username) {
        List<CouncilWorkView> work = new java.util.ArrayList<>();
        for (CouncilTopic councilTopic : councilTopicRepository.findActiveWorkForLecturer(username)) {
            work.add(toWorkView(councilTopic, username));
        }
        return work;
    }

    /** Lưu hoặc cập nhật một điểm cho một đề tài trong hội đồng. */
    @Transactional
    public void submitScore(Long councilTopicId, String username, ReviewScoreForm form) {
        CouncilTopic councilTopic = getCouncilTopic(councilTopicId);
        requireActiveCouncil(councilTopic.getCouncil());
        CouncilMember member = getMember(councilTopic.getCouncil().getId(), username);
        if (isSupervisor(councilTopic, member.getLecturer().getId())) {
            throw new IllegalStateException("GVHD không được chấm đề tài mình hướng dẫn.");
        }
        Result existingResult = resultRepository.findByTopicRegistrationId(
                councilTopic.getTopicRegistration().getId()).orElse(null);
        if (existingResult != null && existingResult.isPublished()) {
            throw new IllegalStateException("Không thể thay đổi điểm sau khi kết quả đã công bố.");
        }
        BigDecimal value = form.getScore();
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("Điểm phải nằm trong khoảng 0 đến 10.");
        }
        Score score = scoreRepository.findByCouncilTopicIdAndLecturerId(councilTopicId, member.getLecturer().getId())
                .orElseGet(() -> {
                    Score fresh = new Score();
                    fresh.setCouncilTopic(councilTopic);
                    fresh.setLecturer(member.getLecturer());
                    return fresh;
                });
        score.setScore(value.setScale(2, RoundingMode.HALF_UP));
        score.setComment(form.getComment() == null ? null : form.getComment().trim());
        scoreRepository.save(score);
    }

    /** Chủ tịch tạo/cập nhật kết quả sau khi đủ mọi thành viên hội đồng đã chấm. */
    @Transactional
    public void finalizeResult(Long councilTopicId, String username, CouncilResultForm form) {
        CouncilTopic councilTopic = getCouncilTopic(councilTopicId);
        requireActiveCouncil(councilTopic.getCouncil());
        requireChair(councilTopic.getCouncil().getId(), username);
        List<CouncilMember> members = councilMemberRepository.findByCouncilId(councilTopic.getCouncil().getId());
        List<Score> scores = scoreRepository.findByCouncilTopicId(councilTopicId);
        int eligibleMemberCount = countEligibleMembers(councilTopic, members);
        if (eligibleMemberCount == 0) {
            throw new IllegalStateException("Không có thành viên hội đồng đủ điều kiện chấm đề tài này.");
        }
        boolean everyMemberScored = true;
        for (CouncilMember member : members) {
            if (isSupervisor(councilTopic, member.getLecturer().getId())) {
                continue; // GVHD được miễn chấm chính đề tài mình hướng dẫn.
            }
            boolean memberHasScore = false;
            for (Score score : scores) {
                if (score.getLecturer().getId().equals(member.getLecturer().getId())) {
                    memberHasScore = true;
                    break;
                }
            }
            if (!memberHasScore) {
                everyMemberScored = false;
                break;
            }
        }
        if (scores.size() != eligibleMemberCount || !everyMemberScored) {
            throw new IllegalStateException("Chưa đủ điểm của tất cả thành viên hội đồng đủ điều kiện chấm.");
        }
        for (Score score : scores) {
            if (isSupervisor(councilTopic, score.getLecturer().getId())) {
                throw new IllegalStateException("Dữ liệu có GVHD chấm đề tài của mình; cần sửa trước khi tổng hợp.");
            }
        }
        RegistrationScoreSummary summary = scoreRepository
                .findAverageForRegistration(councilTopic.getTopicRegistration().getId())
                .orElseThrow(() -> new IllegalStateException("Chưa có điểm thành phần để tính trung bình."));
        if (summary.getScoreCount() == null || summary.getScoreCount() != eligibleMemberCount) {
            throw new IllegalStateException("Số điểm trung bình không khớp số thành viên đủ điều kiện chấm.");
        }

        TopicRegistration registration = councilTopic.getTopicRegistration();
        Result result = resultRepository.findByTopicRegistrationId(registration.getId()).orElse(null);
        if (result == null) {
            result = new Result();
        }
        if (result.isPublished()) throw new IllegalStateException("Không thể sửa kết quả đã công bố.");
        result.setTopicRegistration(registration);
        result.setFinalScore(summary.getAvgScore().setScale(2, RoundingMode.HALF_UP));
        result.setResult(form.getResult().trim());
        result.setCouncilComment(form.getCouncilComment().trim());
        result.setPublished(false);
        result.setPublishedAt(null);
        resultRepository.save(result);
    }

    /** HEAD công bố kết quả đã được chủ tịch tổng hợp. */
    @Transactional
    public void publishResultByHead(Long councilTopicId, String username) {
        User publisher = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (publisher.getRole() != UserRole.HEAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Khoa/Trưởng khoa được công bố kết quả.");
        }
        CouncilTopic councilTopic = getCouncilTopic(councilTopicId);
        requireActiveCouncil(councilTopic.getCouncil());
        Result result = resultRepository.findByTopicRegistrationId(councilTopic.getTopicRegistration().getId())
                .orElseThrow(() -> new IllegalStateException("Chủ tịch cần tổng hợp kết quả trước khi Khoa công bố."));
        if (result.isPublished()) throw new IllegalStateException("Kết quả này đã được công bố.");
        result.setPublished(true);
        result.setPublishedAt(now());
        resultRepository.save(result);

        List<CouncilTopic> allTopics = councilTopicRepository.findByCouncilId(councilTopic.getCouncil().getId());
        boolean allPublished = !allTopics.isEmpty();
        for (CouncilTopic topic : allTopics) {
            Result topicResult = resultRepository.findByTopicRegistrationId(
                    topic.getTopicRegistration().getId()).orElse(null);
            if (topicResult == null || !topicResult.isPublished()) {
                allPublished = false;
                break;
            }
        }
        if (allPublished) {
            Council council = councilTopic.getCouncil();
            council.setStatus(CouncilStatus.COMPLETED);
            councilRepository.save(council);
        }
    }

    /** Returns consolidated results waiting for the faculty's publication step. */
    @Transactional(readOnly = true)
    public List<HeadResultView> getResultsAwaitingPublication() {
        List<HeadResultView> pending = new java.util.ArrayList<>();
        for (Result result : resultRepository.findByPublishedFalseOrderByIdAsc()) {
            CouncilTopic councilTopic = councilTopicRepository
                    .findByTopicRegistrationId(result.getTopicRegistration().getId()).orElse(null);
            if (councilTopic != null && councilTopic.getCouncil().getStatus() == CouncilStatus.ACTIVE) {
                TopicRegistration registration = result.getTopicRegistration();
                pending.add(new HeadResultView(councilTopic.getId(), councilTopic.getCouncil().getName(),
                        registration.getStudentGroup().getName(), registration.getTopic().getTitle(),
                        result.getFinalScore(), result.getResult(), result.getCouncilComment()));
            }
        }
        return pending;
    }

    /** Sinh viên chỉ xem kết quả công bố của đăng ký thuộc nhóm mình. */
    @Transactional(readOnly = true)
    public List<StudentResultView> getResultsForStudent(String username) {
        GroupMember membership = groupMemberRepository.findByStudentUsername(username).orElse(null);
        if (membership == null) return List.of();
        TopicRegistration registration = registrationRepository.findByStudentGroup_Id(membership.getStudentGroup().getId())
                .orElse(null);
        if (registration == null) return List.of();
        List<StudentResultView> views = new java.util.ArrayList<>();
        Result result = resultRepository.findByTopicRegistrationId(registration.getId()).orElse(null);
        if (result != null && result.isPublished()) {
            views.add(new StudentResultView(registration.getStudentGroup().getName(),
                    registration.getTopic().getTitle(), result.getFinalScore(), result.getResult(),
                    result.getCouncilComment(), result.getPublishedAt()));
        }
        return views;
    }

    private CouncilWorkView toWorkView(CouncilTopic topic, String username) {
        List<CouncilMember> members = councilMemberRepository.findByCouncilId(topic.getCouncil().getId());
        CouncilMember current = null;
        for (CouncilMember member : members) {
            if (member.getLecturer().getUsername().equals(username)) {
                current = member;
                break;
            }
        }
        List<Score> scores = scoreRepository.findByCouncilTopicId(topic.getId());
        Score ownScore = null;
        if (current != null) {
            for (Score score : scores) {
                if (score.getLecturer().getId().equals(current.getLecturer().getId())) {
                    ownScore = score;
                    break;
                }
            }
        }
        RegistrationScoreSummary scoreSummary = scoreRepository
                .findAverageForRegistration(topic.getTopicRegistration().getId()).orElse(null);
        BigDecimal average = scoreSummary == null ? null : scoreSummary.getAvgScore();
        Result result = resultRepository.findByTopicRegistrationId(topic.getTopicRegistration().getId()).orElse(null);
        boolean supervisor = current != null && isSupervisor(topic, current.getLecturer().getId());
        boolean isPublished = result != null && result.isPublished();
        int eligibleMemberCount = countEligibleMembers(topic, members);
        boolean allScored = allEligibleMembersScored(topic, members, scores);
        List<String> scoreLabels = new java.util.ArrayList<>();
        for (Score score : scores) {
            scoreLabels.add(score.getLecturer().getFullName() + ": " + score.getScore());
        }
        return new CouncilWorkView(topic.getId(), topic.getTopicRegistration().getId(), topic.getCouncil().getName(),
                topic.getTopicRegistration().getStudentGroup().getName(), topic.getTopicRegistration().getTopic().getTitle(),
                current == null ? null : current.getRole(),
                scoreLabels,
                scores.size(), eligibleMemberCount, average, ownScore == null ? null : ownScore.getScore(),
                current != null && !supervisor && !isPublished, current != null && current.getRole() == CouncilRole.CHAIRMAN
                        && allScored && !isPublished,
                result == null ? null : result.getResult(), result == null ? null : result.getCouncilComment(),
                isPublished, reportService.isDownloadAvailable(topic.getTopicRegistration().getId()));
    }

    private int countEligibleMembers(CouncilTopic topic, List<CouncilMember> members) {
        int eligible = 0;
        for (CouncilMember member : members) {
            if (!isSupervisor(topic, member.getLecturer().getId())) {
                eligible++;
            }
        }
        return eligible;
    }

    private boolean allEligibleMembersScored(CouncilTopic topic, List<CouncilMember> members, List<Score> scores) {
        int eligibleCount = 0;
        for (CouncilMember member : members) {
            if (isSupervisor(topic, member.getLecturer().getId())) {
                continue;
            }
            eligibleCount++;
            boolean foundScore = false;
            for (Score score : scores) {
                if (score.getLecturer().getId().equals(member.getLecturer().getId())) {
                    foundScore = true;
                    break;
                }
            }
            if (!foundScore) {
                return false;
            }
        }
        if (eligibleCount == 0 || scores.size() != eligibleCount) {
            return false;
        }
        for (Score score : scores) {
            if (isSupervisor(topic, score.getLecturer().getId())) {
                return false;
            }
        }
        return true;
    }

    private boolean isSupervisor(CouncilTopic councilTopic, Long lecturerId) {
        List<TopicSupervisor> supervisors = supervisorRepository.findByTopicIds(
                List.of(councilTopic.getTopicRegistration().getTopic().getId()));
        for (TopicSupervisor supervisor : supervisors) {
            if (supervisor.getLecturer().getId().equals(lecturerId)) {
                return true;
            }
        }
        return false;
    }

    private CouncilTopic getCouncilTopic(Long id) {
        return councilTopicRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề tài trong hội đồng."));
    }

    private CouncilMember getMember(Long councilId, String username) {
        for (CouncilMember member : councilMemberRepository.findByCouncilId(councilId)) {
            if (member.getLecturer().getUsername().equals(username)) {
                return member;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thuộc hội đồng này.");
    }

    private CouncilMember requireChair(Long councilId, String username) {
        CouncilMember member = getMember(councilId, username);
        if (member.getRole() != CouncilRole.CHAIRMAN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ chủ tịch hội đồng được tổng hợp kết quả.");
        }
        return member;
    }

    private void requireActiveCouncil(Council council) {
        if (council.getStatus() != CouncilStatus.ACTIVE) throw new IllegalStateException("Hội đồng chưa hoạt động.");
    }

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
}
