package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Thành viên của nhóm; database bảo đảm mỗi sinh viên chỉ thuộc một nhóm. */
@Entity
@Table(name = "group_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_student_in_group", columnNames = "student_id"),
        @UniqueConstraint(name = "uq_group_student", columnNames = {"group_id", "student_id"})
})
public class GroupMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudentGroup studentGroup;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    protected GroupMember() { }
    public GroupMember(StudentGroup group, User student) { this.studentGroup = group; this.student = student; }
    public Long getId() { return id; }
    public StudentGroup getStudentGroup() { return studentGroup; }
    public User getStudent() { return student; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
