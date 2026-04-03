package com.example.campus.repository;

import com.example.campus.entity.ReportConsumeWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportConsumeWeeklyRepository extends JpaRepository<ReportConsumeWeekly, Long> {
    List<ReportConsumeWeekly> findByWeek(String week);
    Optional<ReportConsumeWeekly> findByWeekAndStudentId(String week, String studentId);
}
