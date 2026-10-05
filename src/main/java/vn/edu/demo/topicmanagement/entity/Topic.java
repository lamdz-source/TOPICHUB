package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Ứng với bảng "topics". */
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    // Đề tài thuộc đợt đăng ký nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_period_id")
    private RegistrationPeriod registrationPeriod;

    // Giảng viên tạo đề tài
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Enumerated(EnumType.STRING)
    private TopicStatus status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Department getDepartment() { return department; }
    public RegistrationPeriod getRegistrationPeriod() { return registrationPeriod; }
    public User getCreatedBy() { return createdBy; }
    public TopicStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDepartment(Department department) { this.department = department; }
    public void setRegistrationPeriod(RegistrationPeriod p) { this.registrationPeriod = p; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public void setStatus(TopicStatus status) { this.status = status; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}
