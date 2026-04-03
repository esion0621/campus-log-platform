package com.example.campus.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class ReportConsumeWeeklyPK implements Serializable {
    private String week;
    private String studentId;
}
