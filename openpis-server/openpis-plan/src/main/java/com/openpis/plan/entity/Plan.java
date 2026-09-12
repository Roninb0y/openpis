package com.openpis.plan.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan")
@Data
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 计划类型：daily 日计划（生效期内每天生效）/ weekly 周计划（按星期生效）
     */
    @Column(name = "plan_type", nullable = false, length = 32)
    private String planType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 时间段配置 JSON：
     * {"items":[{"programId":1,"dayOfWeek":1,"startTime":"08:00","endTime":"12:00"},...]}
     * daily 计划忽略 dayOfWeek；weekly 计划 dayOfWeek 1~7（周一~周日）
     */
    @Column(nullable = false, columnDefinition = "JSON")
    private String schedule;

    /**
     * 0-草稿 1-已发布
     */
    @Column(nullable = false)
    private Integer status = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
