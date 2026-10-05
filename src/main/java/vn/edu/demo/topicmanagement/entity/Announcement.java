package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 500) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String content;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AnnouncementScope scope = AnnouncementScope.SCHOOL;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "department_id") private Department department;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AnnouncementStatus status = AnnouncementStatus.DRAFT;
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public User getCreatedBy() { return createdBy; }
    public AnnouncementScope getScope() { return scope; }
    public Department getDepartment() { return department; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public AnnouncementStatus getStatus() { return status; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedBy(User user) { this.createdBy = user; }
    public void setScope(AnnouncementScope scope) { this.scope = scope; }
    public void setDepartment(Department department) { this.department = department; }
    public void setPublishedAt(LocalDateTime date) { this.publishedAt = date; }
    public void setStatus(AnnouncementStatus status) { this.status = status; }
}
