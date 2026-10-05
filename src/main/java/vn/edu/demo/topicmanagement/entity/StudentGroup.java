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

/** Nhóm thực hiện đề tài; quy tắc tối đa 3 thành viên được kiểm tra tại service. */
@Entity
@Table(name = "student_groups")
public class StudentGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_leader_id", nullable = false)
    private User groupLeader;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupStatus status = GroupStatus.ACTIVE;
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public User getGroupLeader() { return groupLeader; }
    public GroupStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setName(String name) { this.name = name; }
    public void setGroupLeader(User groupLeader) { this.groupLeader = groupLeader; }
    public void setStatus(GroupStatus status) { this.status = status; }
}
