package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_tag")
@Data
public class StudentTag {
    @Id
    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(name = "consumption_level", length = 20)
    private String consumptionLevel;

    @Column(name = "interest_tags", length = 200)
    private String interestTags;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;
}
