package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "councils")
public class Council {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "registration_period_id", nullable = false)
    private RegistrationPeriod registrationPeriod;
    @Column(name = "defense_date") private LocalDateTime defenseDate;
    private String room;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CouncilStatus status = CouncilStatus.DRAFT;
    public Long getId() { return id; }
    public String getName() { return name; }
    public RegistrationPeriod getRegistrationPeriod() { return registrationPeriod; }
    public LocalDateTime getDefenseDate() { return defenseDate; }
    public String getRoom() { return room; }
    public CouncilStatus getStatus() { return status; }
    public void setName(String name) { this.name = name; }
    public void setRegistrationPeriod(RegistrationPeriod period) { this.registrationPeriod = period; }
    public void setDefenseDate(LocalDateTime defenseDate) { this.defenseDate = defenseDate; }
    public void setRoom(String room) { this.room = room; }
    public void setStatus(CouncilStatus status) { this.status = status; }
}
