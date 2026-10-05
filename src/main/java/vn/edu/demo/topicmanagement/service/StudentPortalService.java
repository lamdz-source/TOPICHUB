package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.StudentGroupView;
import vn.edu.demo.topicmanagement.dto.StudentGroupMemberView;
import vn.edu.demo.topicmanagement.dto.PendingRegistrationView;
import vn.edu.demo.topicmanagement.dto.StudentRegistrationView;
import vn.edu.demo.topicmanagement.dto.TopicOption;
import vn.edu.demo.topicmanagement.entity.*;
import vn.edu.demo.topicmanagement.repository.*;

/** Nghiệp vụ lập nhóm, đăng ký đề tài và duyệt đăng ký. */
@Service
public class StudentPortalService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_GROUP_SIZE = 3;

    private final StudentGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final TopicRegistrationRepository registrationRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    public StudentPortalService(StudentGroupRepository groupRepository,
                                GroupMemberRepository memberRepository,
                                TopicRegistrationRepository registrationRepository,
                                TopicRepository topicRepository,
                                UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.registrationRepository = registrationRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<StudentGroupView> getMyGroup(String username) {
        return memberRepository.findByStudentUsername(username).map(member -> toGroupView(member.getStudentGroup(), username));
    }

    @Transactional(readOnly = true)
    public List<StudentGroupView> getJoinableGroups(String username) {
        if (memberRepository.findByStudentUsername(username).isPresent()) return List.of();
        return groupRepository.findByStatusOrderByIdDesc(GroupStatus.ACTIVE).stream()
                .filter(group -> registrationRepository.findByStudentGroup_Id(group.getId()).isEmpty())
                .filter(group -> memberRepository.countByStudentGroup_Id(group.getId()) < MAX_GROUP_SIZE)
                .map(group -> toGroupView(group, username))
                .toList();
    }

    @Transactional
    public void createGroup(String name, String username) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Vui lòng nhập tên nhóm.");
        String trimmedName = name.trim();
        if (trimmedName.length() > 255) throw new IllegalArgumentException("Tên nhóm tối đa 255 ký tự.");
        User student = getActiveStudent(username);
        requireNotInAGroup(student);

        StudentGroup group = new StudentGroup();
        group.setName(trimmedName);
        group.setGroupLeader(student);
        group.setStatus(GroupStatus.ACTIVE);
        groupRepository.save(group);
        memberRepository.save(new GroupMember(group, student));
    }

    @Transactional
    public void joinGroup(Long groupId, String username) {
        User student = getActiveStudent(username);
        requireNotInAGroup(student);
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhóm."));
        if (group.getStatus() != GroupStatus.ACTIVE) throw new IllegalStateException("Nhóm này hiện không nhận thành viên.");
        if (registrationRepository.findByStudentGroup_Id(groupId).isPresent()) {
            throw new IllegalStateException("Nhóm đã đăng ký đề tài và không nhận thêm thành viên.");
        }
        if (memberRepository.countByStudentGroup_Id(groupId) >= MAX_GROUP_SIZE) {
            throw new IllegalStateException("Nhóm đã đủ tối đa 3 thành viên.");
        }
        memberRepository.save(new GroupMember(group, student));
    }

    @Transactional
    public void transferGroupLeadership(Long groupId, Long newLeaderId, String username) {
        GroupMember currentMember = memberRepository.findByStudentUsername(username)
                .orElseThrow(() -> new IllegalStateException("Bạn chưa tham gia nhóm."));
        StudentGroup group = currentMember.getStudentGroup();
        if (!group.getId().equals(groupId)) {
            throw new IllegalStateException("Bạn không thuộc nhóm này.");
        }
        if (!group.getGroupLeader().getUsername().equals(username)) {
            throw new IllegalStateException("Chỉ nhóm trưởng hiện tại mới được chuyển quyền trưởng nhóm.");
        }
        if (registrationRepository.findByStudentGroup_Id(groupId).isPresent()) {
            throw new IllegalStateException("Không thể đổi nhóm trưởng sau khi nhóm đã đăng ký đề tài.");
        }
        GroupMember newLeader = memberRepository.findByStudentGroup_Id(groupId).stream()
                .filter(member -> member.getStudent().getId().equals(newLeaderId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nhóm trưởng mới phải là thành viên hiện tại của nhóm."));
        group.setGroupLeader(newLeader.getStudent());
    }

    @Transactional(readOnly = true)
    public List<TopicOption> getAvailableTopics() {
        return topicRepository.findAvailableForStudents(TopicStatus.PUBLISHED, PeriodStatus.OPEN, now()).stream()
                .map(topic -> new TopicOption(topic.getId(), topic.getTitle(), topic.getDepartment().getName()))
                .toList();
    }

    @Transactional
    public void registerTopic(Long topicId, String username) {
        GroupMember membership = memberRepository.findByStudentUsername(username)
                .orElseThrow(() -> new IllegalStateException("Bạn cần tham gia một nhóm trước."));
        StudentGroup group = membership.getStudentGroup();
        if (!group.getGroupLeader().getUsername().equals(username)) {
            throw new IllegalStateException("Chỉ nhóm trưởng mới được đăng ký đề tài.");
        }
        TopicRegistration existingRegistration = registrationRepository.findByStudentGroup_Id(group.getId()).orElse(null);
        if (existingRegistration != null && existingRegistration.getStatus() != RegistrationStatus.REJECTED) {
            throw new IllegalStateException("Nhóm đã có đăng ký đề tài chưa bị từ chối.");
        }
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đề tài."));
        LocalDateTime currentTime = now();
        RegistrationPeriod period = topic.getRegistrationPeriod();
        if (topic.getStatus() != TopicStatus.PUBLISHED || period.getStatus() != PeriodStatus.OPEN
                || currentTime.isBefore(period.getStudentStart()) || currentTime.isAfter(period.getStudentEnd())) {
            throw new IllegalStateException("Đề tài không nằm trong đợt đang mở đăng ký cho sinh viên.");
        }

        TopicRegistration registration = existingRegistration == null ? new TopicRegistration() : existingRegistration;
        registration.setStudentGroup(group);
        registration.setTopic(topic);
        registration.setStatus(RegistrationStatus.PENDING);
        registration.setRejectReason(null);
        registration.setApprovedAt(null);
        registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<StudentRegistrationView> getMyRegistrations(String username) {
        return registrationRepository.findForStudent(username).stream().map(this::toRegistrationView).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingRegistrationView> getPendingRegistrations() {
        return registrationRepository.findAllWithDetailsByStatus(RegistrationStatus.PENDING).stream()
                .map(this::toPendingRegistrationView).toList();
    }

    @Transactional
    public void approveRegistration(Long id) {
        TopicRegistration registration = getPendingRegistration(id);
        registration.setStatus(RegistrationStatus.APPROVED);
        registration.setRejectReason(null);
        registration.setApprovedAt(now());
    }

    @Transactional
    public void rejectRegistration(Long id, String reason) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Vui lòng nhập lý do từ chối.");
        if (reason.trim().length() > 500) throw new IllegalArgumentException("Lý do từ chối tối đa 500 ký tự.");
        TopicRegistration registration = getPendingRegistration(id);
        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setRejectReason(reason.trim());
    }

    private User getActiveStudent(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        if (user.getRole() != UserRole.STUDENT || user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chức năng này dành cho sinh viên đang hoạt động.");
        }
        return user;
    }

    private void requireNotInAGroup(User student) {
        if (memberRepository.existsByStudentId(student.getId())) {
            throw new IllegalStateException("Mỗi sinh viên chỉ được thuộc một nhóm.");
        }
    }

    private StudentGroupView toGroupView(StudentGroup group, String username) {
        List<GroupMember> groupMembers = memberRepository.findByStudentGroup_Id(group.getId());
        List<String> memberNames = groupMembers.stream().map(member -> member.getStudent().getFullName()).toList();
        List<StudentGroupMemberView> members = groupMembers.stream()
                .map(member -> new StudentGroupMemberView(member.getStudent().getId(),
                        member.getStudent().getFullName(), member.getStudent().getStudentCode()))
                .toList();
        boolean leader = group.getGroupLeader().getUsername().equals(username);
        boolean hasRegistration = registrationRepository.findByStudentGroup_Id(group.getId()).isPresent();
        return new StudentGroupView(group.getId(), group.getName(), group.getGroupLeader().getFullName(),
                memberNames, members, leader, leader && !hasRegistration && members.size() > 1);
    }

    private StudentRegistrationView toRegistrationView(TopicRegistration registration) {
        return new StudentRegistrationView(registration.getId(), registration.getStudentGroup().getName(),
                registration.getTopic().getTitle(), registration.getStatus(), registration.getRejectReason(),
                registration.getRegisteredAt());
    }

    private PendingRegistrationView toPendingRegistrationView(TopicRegistration registration) {
        StudentGroup group = registration.getStudentGroup();
        Topic topic = registration.getTopic();
        List<StudentGroupMemberView> members = memberRepository.findByStudentGroup_Id(group.getId()).stream()
                .map(member -> new StudentGroupMemberView(member.getStudent().getId(),
                        member.getStudent().getFullName(), member.getStudent().getStudentCode()))
                .toList();
        return new PendingRegistrationView(registration.getId(), group.getName(),
                group.getGroupLeader().getFullName(), members, topic.getTitle(), topic.getDescription(),
                topic.getDepartment() == null ? "Chưa phân bộ môn" : topic.getDepartment().getName(),
                topic.getRegistrationPeriod().getName(),
                topic.getRegistrationPeriod().getType().getDisplayName(), registration.getRegisteredAt());
    }

    private TopicRegistration getPendingRegistration(Long id) {
        TopicRegistration registration = registrationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đăng ký đề tài."));
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException("Chỉ xử lý đăng ký đang chờ duyệt.");
        }
        return registration;
    }

    private LocalDateTime now() { return LocalDateTime.now(ZONE); }
}
