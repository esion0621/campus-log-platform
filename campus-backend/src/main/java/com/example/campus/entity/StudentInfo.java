package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "student_info")
@Data
public class StudentInfo {
    @Id
    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "college_id", length = 10)
    private String collegeId;

    @Column(name = "major", length = 50)
    private String major;

    @Column(name = "enroll_year")
    private Integer enrollYear;
}
