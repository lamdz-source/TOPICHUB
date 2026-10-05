package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "council_topics", uniqueConstraints =
        @UniqueConstraint(name = "uq_registration_council", columnNames = "topic_registration_id"))
public class CouncilTopic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "council_id", nullable = false)
    private Council council;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "topic_registration_id", nullable = false)
    private TopicRegistration topicRegistration;
    protected CouncilTopic() { }
    public CouncilTopic(Council council, TopicRegistration registration) { this.council = council; this.topicRegistration = registration; }
    public Long getId() { return id; }
    public Council getCouncil() { return council; }
    public TopicRegistration getTopicRegistration() { return topicRegistration; }
}
