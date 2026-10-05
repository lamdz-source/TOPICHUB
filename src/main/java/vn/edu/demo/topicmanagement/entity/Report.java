package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports", uniqueConstraints =
        @UniqueConstraint(name = "uq_report_registration", columnNames = "topic_registration_id"))
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_registration_id", nullable = false)
    private TopicRegistration topicRegistration;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    @Column(name = "file_name", nullable = false) private String fileName;
    @Column(name = "file_path", nullable = false) private String filePath;
    @Column(name = "submitted_at", insertable = false, updatable = false) private LocalDateTime submittedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportStatus status = ReportStatus.SUBMITTED;
    public Long getId() { return id; }
    public TopicRegistration getTopicRegistration() { return topicRegistration; }
    public User getSubmittedBy() { return submittedBy; }
    public String getFileName() { return fileName; }
    public String getFilePath() { return filePath; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public ReportStatus getStatus() { return status; }
    public void setTopicRegistration(TopicRegistration value) { this.topicRegistration = value; }
    public void setSubmittedBy(User value) { this.submittedBy = value; }
    public void setFileName(String value) { this.fileName = value; }
    public void setFilePath(String value) { this.filePath = value; }
    public void setStatus(ReportStatus status) { this.status = status; }
}
