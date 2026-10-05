package vn.edu.demo.topicmanagement.entity;

import jakarta.persistence.*;

/** Entity = 1 class Java đại diện cho 1 bảng. Class này ứng với bảng "departments". */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // id tự tăng (AUTO_INCREMENT)
    private Long id;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}
