package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "scores", uniqueConstraints = @UniqueConstraint(name = "uq_score_lecturer_topic",
        columnNames = {"council_topic_id", "lecturer_id"}))
public class Score {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "council_topic_id", nullable = false)
    private CouncilTopic councilTopic;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;
    @Column(nullable = false, precision = 4, scale = 2) private BigDecimal score;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
    public Long getId() { return id; }
    public CouncilTopic getCouncilTopic() { return councilTopic; }
    public User getLecturer() { return lecturer; }
    public BigDecimal getScore() { return score; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCouncilTopic(CouncilTopic value) { this.councilTopic = value; }
    public void setLecturer(User value) { this.lecturer = value; }
    public void setScore(BigDecimal score) { this.score = score; }
    public void setComment(String comment) { this.comment = comment; }
}
