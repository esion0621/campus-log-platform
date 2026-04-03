package com.example.campus.controller;

import com.example.campus.dto.LogEntryDTO;
import com.example.campus.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final RedisService redisService;

    @GetMapping("/{topic}")
    public List<LogEntryDTO> getLatestLogs(@PathVariable String topic) {
        String redisKey = "latest_logs:" + topic;
        List<String> logs = redisService.lrange(redisKey, 0, 9);
        return logs.stream().map(json -> {
            LogEntryDTO dto = new LogEntryDTO();
            dto.setTopic(topic);
            dto.setJson(json);
            return dto;
        }).collect(Collectors.toList());
    }
}
