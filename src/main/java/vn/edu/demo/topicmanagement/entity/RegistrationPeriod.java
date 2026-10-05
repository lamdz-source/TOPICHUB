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
import java.time.LocalDateTime;

/** Ứng với bảng registration_periods; hai giai đoạn GV/SV và các hạn tùy theo loại đợt. */
@Entity
@Table(name = "registration_periods")
public class RegistrationPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PeriodType type;

    @Column(name = "lecturer_start", nullable = false)
    private LocalDateTime lecturerStart;

    @Column(name = "lecturer_end", nullable = false)
    private LocalDateTime lecturerEnd;

    @Column(name = "student_start", nullable = false)
    private LocalDateTime studentStart;

    @Column(name = "student_end", nullable = false)
    private LocalDateTime studentEnd;

    @Column(name = "review_deadline")
    private LocalDateTime reviewDeadline;

    @Column(name = "defense_date")
    private LocalDateTime defenseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public PeriodType getType() { return type; }
    public LocalDateTime getLecturerStart() { return lecturerStart; }
    public LocalDateTime getLecturerEnd() { return lecturerEnd; }
    public LocalDateTime getStudentStart() { return studentStart; }
    public LocalDateTime getStudentEnd() { return studentEnd; }
    public LocalDateTime getReviewDeadline() { return reviewDeadline; }
    public LocalDateTime getDefenseDate() { return defenseDate; }
    public PeriodStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(PeriodType type) { this.type = type; }
    public void setLecturerStart(LocalDateTime lecturerStart) { this.lecturerStart = lecturerStart; }
    public void setLecturerEnd(LocalDateTime lecturerEnd) { this.lecturerEnd = lecturerEnd; }
    public void setStudentStart(LocalDateTime studentStart) { this.studentStart = studentStart; }
    public void setStudentEnd(LocalDateTime studentEnd) { this.studentEnd = studentEnd; }
    public void setReviewDeadline(LocalDateTime reviewDeadline) { this.reviewDeadline = reviewDeadline; }
    public void setDefenseDate(LocalDateTime defenseDate) { this.defenseDate = defenseDate; }
    public void setStatus(PeriodStatus status) { this.status = status; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
}
