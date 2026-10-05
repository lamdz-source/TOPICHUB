package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;

/**
 * Ứng với bảng "topic_supervisors": mỗi dòng là 1 giảng viên hướng dẫn (GVHD) của 1 đề tài.
 * Mỗi đề tài có 1 đến 2 dòng như vậy; người tạo đề tài là GVHD chính.
 */
@Entity
@Table(name = "topic_supervisors")
public class TopicSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id")
    private User lecturer;

    @Column(name = "is_primary")
    private boolean primarySupervisor;

    protected TopicSupervisor() { }   // JPA bắt buộc phải có constructor rỗng

    public TopicSupervisor(Topic topic, User lecturer, boolean primarySupervisor) {
        this.topic = topic;
        this.lecturer = lecturer;
        this.primarySupervisor = primarySupervisor;
    }

    public Long getId() { return id; }
    public Topic getTopic() { return topic; }
    public User getLecturer() { return lecturer; }
    public boolean isPrimarySupervisor() { return primarySupervisor; }
}
