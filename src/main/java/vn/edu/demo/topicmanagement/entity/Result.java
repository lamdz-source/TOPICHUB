package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "results", uniqueConstraints =
        @UniqueConstraint(name = "uq_result_registration", columnNames = "topic_registration_id"))
public class Result {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_registration_id", nullable = false)
    private TopicRegistration topicRegistration;
    @Column(name = "final_score", nullable = false, precision = 4, scale = 2) private BigDecimal finalScore;
    private String result;
    @Column(name = "council_comment", columnDefinition = "TEXT") private String councilComment;
    @Column(nullable = false) private boolean published;
    @Column(name = "published_at") private LocalDateTime publishedAt;
    public Long getId() { return id; }
    public TopicRegistration getTopicRegistration() { return topicRegistration; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getResult() { return result; }
    public String getCouncilComment() { return councilComment; }
    public boolean isPublished() { return published; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setTopicRegistration(TopicRegistration value) { this.topicRegistration = value; }
    public void setFinalScore(BigDecimal score) { this.finalScore = score; }
    public void setResult(String result) { this.result = result; }
    public void setCouncilComment(String value) { this.councilComment = value; }
    public void setPublished(boolean published) { this.published = published; }
    public void setPublishedAt(LocalDateTime value) { this.publishedAt = value; }
}
