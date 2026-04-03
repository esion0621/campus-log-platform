package com.example.campus.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RealtimeDTO {
    private String libraryCount;
    private List<String> libraryTrend;
    private String eduQps;
    private String canteenTotal10s;
    private String eduOnlineUsers;
    private String deviceOnlineCount;
    private String deviceTotalCount;
    private Map<String, String> canteenDailyTotal;
    private Map<String, String> canteenDailyShare;
    private Map<String, String> itemDailyCount;
    private Map<String, String> itemDailyTotal;
    private Map<String, String> deviceStatus;
    private Map<String, String> borrowCategoryCount;
}
