package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.io.Serializable;

@Entity
@Table(name = "report_library_weekly")
@IdClass(ReportLibraryWeeklyPK.class)
@Data
public class ReportLibraryWeekly {
    @Id
    @Column(name = "week", length = 10)
    private String week;

    @Id
    @Column(name = "college_id", length = 10)
    private String collegeId;

    @Column(name = "access_count")
    private Long accessCount;
}
