package com.example.campus.service;

import com.example.campus.dto.LibraryRankDTO;
import com.example.campus.entity.College;
import com.example.campus.entity.ReportConsumeWeekly;
import com.example.campus.entity.ReportLibraryWeekly;
import com.example.campus.repository.CollegeRepository;
import com.example.campus.repository.ReportConsumeWeeklyRepository;
import com.example.campus.repository.ReportLibraryWeeklyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportLibraryWeeklyRepository libraryWeeklyRepo;
    private final ReportConsumeWeeklyRepository consumeWeeklyRepo;
    private final CollegeRepository collegeRepo;

    public List<LibraryRankDTO> getLibraryWeeklyRank(String week) {
        List<ReportLibraryWeekly> records = libraryWeeklyRepo.findByWeek(week);
        return records.stream().map(rec -> {
            College college = collegeRepo.findById(rec.getCollegeId()).orElse(null);
            String collegeName = college != null ? college.getCollegeName() : rec.getCollegeId();
            return new LibraryRankDTO(rec.getCollegeId(), collegeName, rec.getAccessCount());
        }).collect(Collectors.toList());
    }

    public List<ReportConsumeWeekly> getConsumeWeekly(String week) {
        return consumeWeeklyRepo.findByWeek(week);
    }

    public ReportConsumeWeekly getStudentConsumeWeekly(String week, String studentId) {
        return consumeWeeklyRepo.findByWeekAndStudentId(week, studentId).orElse(null);
    }
}
