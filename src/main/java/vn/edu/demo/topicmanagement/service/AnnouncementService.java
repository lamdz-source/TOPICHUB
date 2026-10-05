package vn.edu.demo.topicmanagement.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.demo.topicmanagement.dto.AnnouncementForm;
import vn.edu.demo.topicmanagement.dto.AnnouncementView;
import vn.edu.demo.topicmanagement.entity.*;
import vn.edu.demo.topicmanagement.repository.AnnouncementRepository;
import vn.edu.demo.topicmanagement.repository.DepartmentRepository;
import vn.edu.demo.topicmanagement.repository.UserRepository;

/** Tạo thông báo nháp, công bố và chỉ hiển thị thông báo phù hợp với bộ môn người đọc. */
@Service
public class AnnouncementService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, UserRepository userRepository,
                               DepartmentRepository departmentRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementView> getVisible(String username) {
        User reader = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        List<Announcement> visible = new ArrayList<>();
        List<Announcement> published = announcementRepository
                .findByStatusOrderByPublishedAtDesc(AnnouncementStatus.PUBLISHED);
        for (Announcement announcement : published) {
            if (announcement.getScope() == AnnouncementScope.SCHOOL) {
                visible.add(announcement);
            }
        }
        if (reader.getDepartment() != null) {
            visible.addAll(announcementRepository.findByStatusAndDepartmentIdOrderByPublishedAtDesc(
                    AnnouncementStatus.PUBLISHED, reader.getDepartment().getId()));
        }
        // Sort explicitly instead of passing method references through Comparator/Stream
        // descriptors; Eclipse's null analysis otherwise reports an unchecked conversion.
        for (int i = 1; i < visible.size(); i++) {
            Announcement item = visible.get(i);
            int position = i - 1;
            while (position >= 0 && publishedAfter(item, visible.get(position))) {
                visible.set(position + 1, visible.get(position));
                position--;
            }
            visible.set(position + 1, item);
        }

        List<AnnouncementView> views = new ArrayList<>();
        for (Announcement announcement : visible) {
            views.add(toView(announcement));
        }
        return views;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementView> getManageList(String username) {
        List<Announcement> announcements = announcementRepository.findByCreatedByUsernameOrderByCreatedAtDesc(username);
        List<AnnouncementView> views = new ArrayList<>();
        for (Announcement announcement : announcements) {
            views.add(toView(announcement));
        }
        return views;
    }

    @Transactional
    public void createDraft(AnnouncementForm form, String username) {
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."));
        Announcement announcement = new Announcement();
        announcement.setCreatedBy(creator);
        announcement.setStatus(AnnouncementStatus.DRAFT);
        applyForm(announcement, form, creator);
        announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public AnnouncementForm getDraftForm(Long id, String username) {
        Announcement announcement = getOwnedDraft(id, username);
        AnnouncementForm form = new AnnouncementForm();
        form.setTitle(announcement.getTitle());
        form.setContent(announcement.getContent());
        form.setScope(announcement.getScope());
        return form;
    }

    @Transactional
    public void updateDraft(Long id, AnnouncementForm form, String username) {
        Announcement announcement = getOwnedDraft(id, username);
        applyForm(announcement, form, announcement.getCreatedBy());
    }

    @Transactional
    public void deleteDraft(Long id, String username) {
        Announcement announcement = getOwnedDraft(id, username);
        announcementRepository.delete(announcement);
    }

    @Transactional
    public void publish(Long id, String username) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));
        if (!announcement.getCreatedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không tạo thông báo này.");
        }
        if (announcement.getStatus() != AnnouncementStatus.DRAFT) {
            throw new IllegalStateException("Chỉ công bố thông báo đang ở trạng thái nháp.");
        }
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setPublishedAt(LocalDateTime.now(ZONE));
    }

    private AnnouncementView toView(Announcement announcement) {
        return new AnnouncementView(announcement.getId(), announcement.getTitle(), announcement.getContent(),
                announcement.getCreatedBy().getFullName(),
                announcement.getDepartment() == null ? null : announcement.getDepartment().getName(),
                announcement.getScope(), announcement.getStatus(), announcement.getCreatedAt(),
                announcement.getPublishedAt());
    }

    private Announcement getOwnedDraft(Long id, String username) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo."));
        if (!announcement.getCreatedBy().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không quản lý thông báo này.");
        }
        if (announcement.getStatus() != AnnouncementStatus.DRAFT) {
            throw new IllegalStateException("Chỉ có thể sửa hoặc xóa thông báo đang ở trạng thái nháp.");
        }
        return announcement;
    }

    private void applyForm(Announcement announcement, AnnouncementForm form, User creator) {
        if (creator.getRole() != UserRole.ADMIN && creator.getRole() != UserRole.HEAD) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin/Trưởng khoa được quản lý thông báo.");
        }
        if (creator.getRole() == UserRole.ADMIN && form.getScope() != AnnouncementScope.SCHOOL) {
            throw new IllegalArgumentException("Admin chỉ tạo được thông báo cấp trường.");
        }
        Department department = null;
        if (form.getScope() == AnnouncementScope.DEPARTMENT) {
            if (creator.getRole() != UserRole.HEAD || creator.getDepartment() == null) {
                throw new IllegalArgumentException("Thông báo cấp khoa cần tài khoản Trưởng khoa có bộ môn.");
            }
            department = departmentRepository.getReferenceById(creator.getDepartment().getId());
        }
        announcement.setTitle(form.getTitle().trim());
        announcement.setContent(form.getContent().trim());
        announcement.setScope(form.getScope());
        announcement.setDepartment(department);
    }

    /** Returns true when left should appear before right in newest-first order. */
    private boolean publishedAfter(Announcement left, Announcement right) {
        LocalDateTime leftTime = left.getPublishedAt();
        LocalDateTime rightTime = right.getPublishedAt();
        if (leftTime == null) return false;
        if (rightTime == null) return true;
        return leftTime.isAfter(rightTime);
    }
}
