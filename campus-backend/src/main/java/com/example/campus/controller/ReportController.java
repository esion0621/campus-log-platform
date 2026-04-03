package com.example.campus.controller;

import com.example.campus.dto.LibraryRankDTO;
import com.example.campus.entity.ReportConsumeWeekly;
import com.example.campus.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/library/weekly-rank")
    public List<LibraryRankDTO> getLibraryWeeklyRank(@RequestParam String week) {
        return reportService.getLibraryWeeklyRank(week);
    }

    @GetMapping("/consume/weekly")
    public List<ReportConsumeWeekly> getConsumeWeekly(@RequestParam String week) {
        return reportService.getConsumeWeekly(week);
    }

    @GetMapping("/consume/student-weekly")
    public ReportConsumeWeekly getStudentConsumeWeekly(
            @RequestParam String week,
            @RequestParam String studentId) {
        return reportService.getStudentConsumeWeekly(week, studentId);
    }
}
