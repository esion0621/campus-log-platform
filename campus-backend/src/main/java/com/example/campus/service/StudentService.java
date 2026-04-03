package com.example.campus.service;

import com.example.campus.dto.StudentProfileDTO;
import com.example.campus.entity.StudentInfo;
import com.example.campus.entity.StudentTag;
import com.example.campus.repository.StudentInfoRepository;
import com.example.campus.repository.StudentTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentInfoRepository studentInfoRepo;
    private final StudentTagRepository studentTagRepo;

    public StudentProfileDTO getStudentProfile(String studentId) {
        StudentInfo info = studentInfoRepo.findById(studentId).orElse(null);
        StudentTag tag = studentTagRepo.findById(studentId).orElse(null);

        if (info == null) return null;

        StudentProfileDTO dto = new StudentProfileDTO();
        dto.setStudentId(studentId);
        dto.setName(info.getName());
        dto.setCollegeId(info.getCollegeId());
        dto.setMajor(info.getMajor());
        dto.setEnrollYear(info.getEnrollYear());

        if (tag != null) {
            dto.setConsumptionLevel(tag.getConsumptionLevel());
            dto.setInterestTags(tag.getInterestTags());
        }
        return dto;
    }
}
