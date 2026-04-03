package com.example.campus.controller;

import com.example.campus.dto.StudentProfileDTO;
import com.example.campus.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/{studentId}/profile")
    public StudentProfileDTO getStudentProfile(@PathVariable String studentId) {
        return studentService.getStudentProfile(studentId);
    }
}
