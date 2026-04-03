package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "college")
@Data
public class College {
    @Id
    @Column(name = "college_id", length = 10)
    private String collegeId;

    @Column(name = "college_name", length = 50)
    private String collegeName;
}
