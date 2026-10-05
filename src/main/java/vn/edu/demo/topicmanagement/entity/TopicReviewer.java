package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "topic_reviewers", uniqueConstraints =
        @UniqueConstraint(name = "uq_reviewer_registration", columnNames = {"topic_registration_id", "lecturer_id"}))
public class TopicReviewer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_registration_id", nullable = false)
    private TopicRegistration topicRegistration;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;
    @Column(name = "assigned_at", insertable = false, updatable = false) private LocalDateTime assignedAt;
    @Column(name = "review_score", precision = 4, scale = 2) private BigDecimal reviewScore;
    @Column(name = "review_comment", columnDefinition = "TEXT") private String reviewComment;
    @Column(name = "review_submitted_at") private LocalDateTime reviewSubmittedAt;
    protected TopicReviewer() { }
    public TopicReviewer(TopicRegistration registration, User lecturer) { this.topicRegistration = registration; this.lecturer = lecturer; }
    public Long getId() { return id; }
    public TopicRegistration getTopicRegistration() { return topicRegistration; }
    public User getLecturer() { return lecturer; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public BigDecimal getReviewScore() { return reviewScore; }
    public String getReviewComment() { return reviewComment; }
    public LocalDateTime getReviewSubmittedAt() { return reviewSubmittedAt; }
    public void setReviewScore(BigDecimal reviewScore) { this.reviewScore = reviewScore; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public void setReviewSubmittedAt(LocalDateTime reviewSubmittedAt) { this.reviewSubmittedAt = reviewSubmittedAt; }
}
