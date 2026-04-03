package com.example.campus.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "report_consume_weekly")
@IdClass(ReportConsumeWeeklyPK.class)
@Data
public class ReportConsumeWeekly {
    @Id
    @Column(name = "week", length = 10)
    private String week;

    @Id
    @Column(name = "student_id", length = 20)
    private String studentId;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "transaction_count")
    private Integer transactionCount;
}
