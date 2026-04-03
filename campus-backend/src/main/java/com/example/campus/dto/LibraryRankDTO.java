package com.example.campus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LibraryRankDTO {
    private String collegeId;
    private String collegeName;
    private Long accessCount;
}
