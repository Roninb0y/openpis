package com.openpis.plan.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PlanDTO {

    // ========== schedule 结构 ==========

    @Data
    public static class Schedule {
        private List<ScheduleItem> items;
    }

    @Data
    public static class ScheduleItem {
        private Long programId;
        /**
         * 仅周计划使用：1~7 表示周一~周日
         */
        private Integer dayOfWeek;
        private String startTime;
        private String endTime;
    }

    // ========== 请求 ==========

    @Data
    public static class CreateRequest {
        private String name;
        private String planType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Schedule schedule;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private String planType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Schedule schedule;
    }

    // ========== 响应 ==========

    @Data
    public static class ListItem {
        private Long id;
        private String name;
        private String planType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer status;
        private Integer itemCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class ScheduleItemDetail {
        private Long programId;
        private String programName;
        private String playType;
        private Integer dayOfWeek;
        private String startTime;
        private String endTime;
    }

    @Data
    public static class Detail {
        private Long id;
        private String name;
        private String planType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer status;
        private List<ScheduleItemDetail> items;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
