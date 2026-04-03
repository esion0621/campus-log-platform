package com.example.campus.dto;

import lombok.Data;

@Data
public class StudentProfileDTO {
    private String studentId;
    private String name;
    private String collegeId;
    private String major;
    private Integer enrollYear;
    private String consumptionLevel;
    private String interestTags;
}
