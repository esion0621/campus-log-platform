package com.example.campus.repository;

import com.example.campus.entity.StudentTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentTagRepository extends JpaRepository<StudentTag, String> {
    Optional<StudentTag> findByStudentId(String studentId);
}
