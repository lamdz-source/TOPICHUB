package vn.edu.demo.topicmanagement.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.ReportDownload;
import vn.edu.demo.topicmanagement.dto.LecturerReportView;
import vn.edu.demo.topicmanagement.dto.StudentReportView;
import vn.edu.demo.topicmanagement.entity.GroupMember;
import vn.edu.demo.topicmanagement.entity.RegistrationStatus;
import vn.edu.demo.topicmanagement.entity.Report;
import vn.edu.demo.topicmanagement.entity.StudentGroup;
import vn.edu.demo.topicmanagement.entity.TopicRegistration;
import vn.edu.demo.topicmanagement.repository.GroupMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilMemberRepository;
import vn.edu.demo.topicmanagement.repository.CouncilTopicRepository;
import vn.edu.demo.topicmanagement.repository.ReportRepository;
import vn.edu.demo.topicmanagement.repository.TopicReviewerRepository;
import vn.edu.demo.topicmanagement.repository.TopicSupervisorRepository;
import vn.edu.demo.topicmanagement.repository.TopicRegistrationRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/**
 * Xử lý nộp và tải báo cáo. Chỉ trưởng nhóm được nộp; thành viên cùng nhóm được tải báo cáo.
 * Tệp được lưu ngoài thư mục static và tên lưu trên đĩa được sinh ngẫu nhiên để tránh ghi đè.
 */
@Service
public class StudentReportService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final GroupMemberRepository memberRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TopicReviewerRepository reviewerRepository;
    private final TopicSupervisorRepository supervisorRepository;
    private final CouncilMemberRepository councilMemberRepository;
    private final CouncilTopicRepository councilTopicRepository;
    private final Path reportDirectory;

    public StudentReportService(GroupMemberRepository memberRepository,
                                TopicRegistrationRepository registrationRepository,
                                ReportRepository reportRepository,
                                UserRepository userRepository,
                                TopicReviewerRepository reviewerRepository,
                                TopicSupervisorRepository supervisorRepository,
                                CouncilMemberRepository councilMemberRepository,
                                CouncilTopicRepository councilTopicRepository,
                                @Value("${app.upload.report-dir:uploads/reports}") String reportDirectory) {
        this.memberRepository = memberRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.reviewerRepository = reviewerRepository;
        this.supervisorRepository = supervisorRepository;
        this.councilMemberRepository = councilMemberRepository;
        this.councilTopicRepository = councilTopicRepository;
        this.reportDirectory = Paths.get(reportDirectory).toAbsolutePath().normalize();
    }

    /** Lấy đề tài hiện tại và trạng thái báo cáo cho sinh viên đang đăng nhập. */
    @Transactional(readOnly = true)
    public StudentReportView getOverview(String username) {
        GroupMember member = memberRepository.findByStudentUsername(username).orElse(null);
        if (member == null) return new StudentReportView(null, null, null, false, null, null, false);

        StudentGroup group = member.getStudentGroup();
        boolean leader = group.getGroupLeader().getUsername().equals(username);
        TopicRegistration registration = registrationRepository.findByStudentGroup_Id(group.getId()).orElse(null);
        if (registration == null) return new StudentReportView(null, null, null, leader, null, null, false);

        Report report = reportRepository.findByTopicRegistrationId(registration.getId()).orElse(null);
        return new StudentReportView(registration.getId(), registration.getTopic().getTitle(), registration.getStatus(),
                leader, report == null ? null : report.getFileName(), report == null ? null : report.getSubmittedAt(),
                report != null && isStoredFileAvailable(report));
    }

    /** Lưu báo cáo một lần cho đăng ký đã được chấp thuận. */
    @Transactional
    public void submit(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tệp báo cáo.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Tệp báo cáo không được vượt quá 10 MB.");

        GroupMember member = memberRepository.findByStudentUsername(username)
                .orElseThrow(() -> new IllegalStateException("Bạn chưa tham gia nhóm."));
        StudentGroup group = member.getStudentGroup();
        if (!group.getGroupLeader().getUsername().equals(username)) {
            throw new IllegalStateException("Chỉ nhóm trưởng mới được nộp báo cáo.");
        }
        TopicRegistration registration = registrationRepository.findByStudentGroup_Id(group.getId())
                .orElseThrow(() -> new IllegalStateException("Nhóm chưa đăng ký đề tài."));
        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new IllegalStateException("Chỉ nộp báo cáo sau khi đăng ký đề tài được duyệt.");
        }
        Report existingReport = reportRepository.findByTopicRegistrationId(registration.getId()).orElse(null);
        if (existingReport != null && isStoredFileAvailable(existingReport)) {
            throw new IllegalStateException("Nhóm đã nộp báo cáo cho đề tài này.");
        }

        String originalName = safeOriginalName(file.getOriginalFilename());
        String extension = getAllowedExtension(originalName);
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = reportDirectory.resolve(storedName).normalize();
        if (!target.startsWith(reportDirectory)) throw new IllegalStateException("Đường dẫn lưu tệp không hợp lệ.");

        try {
            Files.createDirectories(reportDirectory);
            file.transferTo(target);
            Report report = existingReport == null ? new Report() : existingReport;
            report.setTopicRegistration(registration);
            report.setSubmittedBy(userRepository.getReferenceById(member.getStudent().getId()));
            report.setFileName(originalName);
            report.setFilePath(storedName);
            reportRepository.saveAndFlush(report);
        } catch (IOException e) {
            deleteQuietly(target);
            throw new IllegalStateException("Không lưu được tệp báo cáo trên máy chủ.", e);
        } catch (RuntimeException e) {
            // Nếu lưu database thất bại thì xóa tệp đã chép để tránh tệp mồ côi.
            deleteQuietly(target);
            throw e;
        }
    }

    /** Kiểm tra thành viên nhóm trước khi đưa đường dẫn tệp cho controller tải xuống. */
    @Transactional(readOnly = true)
    public ReportDownload getDownload(Long registrationId, String username) {
        GroupMember member = memberRepository.findByStudentUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        TopicRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!member.getStudentGroup().getId().equals(registration.getStudentGroup().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ thành viên trong nhóm mới được tải báo cáo.");
        }
        return resolveDownload(registrationId);
    }

    /** GVPB hoac thanh vien hoi dong chi duoc tai bao cao cua de tai duoc giao cho minh. */
    @Transactional(readOnly = true)
    public ReportDownload getDownloadForLecturer(Long registrationId, String username) {
        boolean assignedReviewer = reviewerRepository.findByTopicRegistrationId(registrationId).stream()
                .anyMatch(row -> row.getLecturer().getUsername().equals(username));
        TopicRegistration registration = registrationRepository.findById(registrationId).orElse(null);
        boolean assignedSupervisor = registration != null
                && !supervisorRepository.findByTopic_IdAndLecturer_Username(
                        registration.getTopic().getId(), username).isEmpty();
        boolean councilMember = councilTopicRepository.findByTopicRegistrationId(registrationId)
                .map(row -> councilMemberRepository.findByCouncilId(row.getCouncil().getId()).stream()
                        .anyMatch(member -> member.getLecturer().getUsername().equals(username)))
                .orElse(false);
        if (!assignedReviewer && !assignedSupervisor && !councilMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phân công xem báo cáo này.");
        }
        return resolveDownload(registrationId);
    }

    /** Used in lecturer lists to avoid offering a download when the file is missing. */
    @Transactional(readOnly = true)
    public boolean isDownloadAvailable(Long registrationId) {
        return reportRepository.findByTopicRegistrationId(registrationId)
                .map(this::isStoredFileAvailable)
                .orElse(false);
    }

    /** Returns submitted reports for topics supervised by the current lecturer. */
    @Transactional(readOnly = true)
    public List<LecturerReportView> getReportsForSupervisor(String username) {
        List<LecturerReportView> views = new java.util.ArrayList<>();
        for (TopicRegistration registration : registrationRepository.findApprovedBySupervisorUsername(username)) {
            Report report = reportRepository.findByTopicRegistrationId(registration.getId()).orElse(null);
            if (report != null) {
                views.add(new LecturerReportView(registration.getId(), registration.getStudentGroup().getName(),
                        registration.getTopic().getTitle(), report.getFileName(), isStoredFileAvailable(report)));
            }
        }
        return views;
    }

    private ReportDownload resolveDownload(Long registrationId) {
        Report report = reportRepository.findByTopicRegistrationId(registrationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có báo cáo."));
        Path path = resolveStoredPath(report);
        if (path == null || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp báo cáo.");
        }
        return new ReportDownload(path, report.getFileName());
    }

    /** Trả về null nếu đường dẫn trong DB nằm ngoài thư mục upload hoặc tệp đã bị mất. */
    private Path resolveStoredPath(Report report) {
        if (report.getFilePath() == null || report.getFilePath().isBlank()) return null;
        Path path = reportDirectory.resolve(report.getFilePath()).normalize();
        return path.startsWith(reportDirectory) ? path : null;
    }

    private boolean isStoredFileAvailable(Report report) {
        Path path = resolveStoredPath(report);
        return path != null && Files.isRegularFile(path);
    }

    private String safeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        String name = Paths.get(originalName).getFileName().toString().trim();
        if (name.length() > 255) throw new IllegalArgumentException("Tên tệp tối đa 255 ký tự.");
        return name;
    }

    private String getAllowedExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 1 || dot == fileName.length() - 1) throw new IllegalArgumentException("Chỉ nhận tệp PDF, DOC hoặc DOCX.");
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Chỉ nhận tệp PDF, DOC hoặc DOCX.");
        return extension;
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { /* Không che lỗi gốc khi dọn tệp tạm. */ }
    }
}
