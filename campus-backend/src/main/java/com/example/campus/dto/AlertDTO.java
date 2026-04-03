package com.example.campus.dto;

import lombok.Data;

@Data
public class AlertDTO {
    private String device;
    private String level;
    private String message;
    private Long time;
}
