package com.example.campus.controller;

import com.example.campus.dto.AlertDTO;
import com.example.campus.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @GetMapping("/latest")
    public List<AlertDTO> getLatestAlerts() {
        List<String> alertsJson = redisService.lrange("alerts:latest", 0, 49);
        List<AlertDTO> alerts = new ArrayList<>();
        for (String json : alertsJson) {
            try {
                AlertDTO dto = objectMapper.readValue(json, AlertDTO.class);
                alerts.add(dto);
            } catch (Exception e) {
                // ignore parse error
            }
        }
        return alerts;
    }
}
