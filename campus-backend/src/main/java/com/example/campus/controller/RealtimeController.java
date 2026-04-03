package com.example.campus.controller;

import com.example.campus.dto.RealtimeDTO;
import com.example.campus.service.RealtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/realtime")
@RequiredArgsConstructor
public class RealtimeController {

    private final RealtimeService realtimeService;

    @GetMapping("/all")
    public RealtimeDTO getAllMetrics() {
        return realtimeService.getRealtimeMetrics();
    }

    @GetMapping("/library/count")
    public String getLibraryCount() {
        return realtimeService.getLibraryCount();
    }

    @GetMapping("/library/trend")
    public List<String> getLibraryTrend() {
        return realtimeService.getLibraryTrend();
    }

    @GetMapping("/edu/qps")
    public String getEduQps() {
        return realtimeService.getEduQps();
    }

    @GetMapping("/canteen/total10s")
    public String getCanteenTotal10s() {
        return realtimeService.getCanteenTotal10s();
    }

    @GetMapping("/edu/onlineUsers")
    public String getEduOnlineUsers() {
        return realtimeService.getEduOnlineUsers();
    }

    @GetMapping("/device/status")
    public Map<Object, Object> getDeviceStatus() {
        return realtimeService.getDeviceStatus();
    }

    @GetMapping("/device/summary")
    public Map<String, String> getDeviceSummary() {
        return realtimeService.getDeviceOnlineSummary();
    }
}
