package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Một nhóm đăng ký thực hiện một đề tài. */
@Entity
@Table(name = "topic_registrations", uniqueConstraints =
        @UniqueConstraint(name = "uq_group_topic_registration", columnNames = "group_id"))
public class TopicRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup studentGroup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.PENDING;
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
    @Column(name = "registered_at", insertable = false, updatable = false)
    private LocalDateTime registeredAt;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public Long getId() { return id; }
    public StudentGroup getStudentGroup() { return studentGroup; }
    public Topic getTopic() { return topic; }
    public RegistrationStatus getStatus() { return status; }
    public String getRejectReason() { return rejectReason; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setStudentGroup(StudentGroup group) { this.studentGroup = group; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
