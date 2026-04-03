package com.example.campus.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class ReportLibraryWeeklyPK implements Serializable {
    private String week;
    private String collegeId;
}
