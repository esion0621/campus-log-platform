package com.example.campus.repository;

import com.example.campus.entity.ReportLibraryWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportLibraryWeeklyRepository extends JpaRepository<ReportLibraryWeekly, Long> {
    List<ReportLibraryWeekly> findByWeek(String week);
}
