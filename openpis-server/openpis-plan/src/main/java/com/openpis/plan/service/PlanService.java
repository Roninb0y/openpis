package com.openpis.plan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openpis.common.event.PlanChangedEvent;
import com.openpis.common.exception.BizException;
import com.openpis.plan.dto.PlanDTO;
import com.openpis.plan.entity.Plan;
import com.openpis.plan.repository.PlanRepository;
import com.openpis.program.entity.Program;
import com.openpis.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final ProgramRepository programRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> PLAN_TYPES = Set.of("daily", "weekly");

    /**
     * 分页查询计划列表
     */
    public Page<PlanDTO.ListItem> list(int page, int size, Integer status, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Plan> plans;

        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasStatus = status != null;
        if (hasStatus && hasKeyword) {
            plans = planRepository.findByNameContainingIgnoreCaseAndStatus(keyword, status, pageable);
        } else if (hasStatus) {
            plans = planRepository.findByStatus(status, pageable);
        } else if (hasKeyword) {
            plans = planRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            plans = planRepository.findAll(pageable);
        }

        return plans.map(this::toListItem);
    }

    /**
     * 创建计划（草稿状态）
     */
    @Transactional
    public PlanDTO.Detail create(PlanDTO.CreateRequest request) {
        validate(request.getPlanType(), request.getStartDate(), request.getEndDate(), request.getSchedule());

        Plan plan = new Plan();
        plan.setName(request.getName());
        plan.setPlanType(request.getPlanType());
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setSchedule(toJsonSchedule(request.getSchedule()));
        plan.setStatus(0);
        plan = planRepository.save(plan);

        return getDetail(plan.getId());
    }

    /**
     * 获取计划详情
     */
    public PlanDTO.Detail getDetail(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new BizException("计划不存在"));
        return toDetail(plan);
    }

    /**
     * 更新计划
     */
    @Transactional
    public PlanDTO.Detail update(Long id, PlanDTO.UpdateRequest request) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new BizException("计划不存在"));

        String planType = StringUtils.hasText(request.getPlanType()) ? request.getPlanType() : plan.getPlanType();
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : plan.getStartDate();
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : plan.getEndDate();
        PlanDTO.Schedule schedule = request.getSchedule() != null
                ? request.getSchedule() : fromJsonSchedule(plan.getSchedule());
        String name = StringUtils.hasText(request.getName()) ? request.getName() : plan.getName();
        validate(planType, startDate, endDate, schedule);

        plan.setName(name);
        plan.setPlanType(planType);
        plan.setStartDate(startDate);
        plan.setEndDate(endDate);
        plan.setSchedule(toJsonSchedule(schedule));
        planRepository.save(plan);

        // 已发布的计划被修改后，通知播放端刷新
        if (plan.getStatus() == 1) {
            eventPublisher.publishEvent(new PlanChangedEvent());
        }
        return getDetail(id);
    }

    /**
     * 删除计划
     */
    @Transactional
    public void delete(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new BizException("计划不存在"));
        planRepository.delete(plan);
        if (plan.getStatus() == 1) {
            eventPublisher.publishEvent(new PlanChangedEvent());
        }
    }

    /**
     * 发布计划
     */
    @Transactional
    public void publish(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new BizException("计划不存在"));
        plan.setStatus(1);
        planRepository.save(plan);
        eventPublisher.publishEvent(new PlanChangedEvent());
        log.info("计划已发布: {} ({})", plan.getName(), plan.getId());
    }

    /**
     * 撤销计划
     */
    @Transactional
    public void unpublish(Long id) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new BizException("计划不存在"));
        plan.setStatus(0);
        planRepository.save(plan);
        eventPublisher.publishEvent(new PlanChangedEvent());
        log.info("计划已撤销: {} ({})", plan.getName(), plan.getId());
    }

    // ========== 校验与转换 ==========

    private void validate(String planType, LocalDate startDate, LocalDate endDate, PlanDTO.Schedule schedule) {
        if (!StringUtils.hasText(planType) || !PLAN_TYPES.contains(planType)) {
            throw new BizException("计划类型必须是 daily 或 weekly");
        }
        if (startDate == null || endDate == null) {
            throw new BizException("生效日期不能为空");
        }
        if (startDate.isAfter(endDate)) {
            throw new BizException("开始日期不能晚于结束日期");
        }
        if (schedule == null || schedule.getItems() == null || schedule.getItems().isEmpty()) {
            throw new BizException("至少需要一条时间段配置");
        }

        Set<Long> programIds = schedule.getItems().stream()
                .map(PlanDTO.ScheduleItem::getProgramId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Program> programMap = programRepository.findAllById(programIds).stream()
                .collect(Collectors.toMap(Program::getId, Function.identity()));

        for (PlanDTO.ScheduleItem item : schedule.getItems()) {
            if (item.getProgramId() == null || !programMap.containsKey(item.getProgramId())) {
                throw new BizException("时间段配置中引用了不存在的栏目");
            }
            if ("weekly".equals(planType)) {
                if (item.getDayOfWeek() == null || item.getDayOfWeek() < 1 || item.getDayOfWeek() > 7) {
                    throw new BizException("周计划的时间段必须指定星期（1~7）");
                }
            }
            LocalTime start = parseTime(item.getStartTime());
            LocalTime end = parseTime(item.getEndTime());
            if (start == null || end == null) {
                throw new BizException("时间格式必须为 HH:mm，如 08:30");
            }
            if (!end.isAfter(start)) {
                throw new BizException("结束时间必须晚于开始时间");
            }
        }
    }

    private LocalTime parseTime(String time) {
        if (!StringUtils.hasText(time)) {
            return null;
        }
        try {
            return LocalTime.parse(time.trim(), TIME_FORMAT);
        } catch (Exception e) {
            return null;
        }
    }

    private PlanDTO.ListItem toListItem(Plan plan) {
        PlanDTO.ListItem dto = new PlanDTO.ListItem();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setPlanType(plan.getPlanType());
        dto.setStartDate(plan.getStartDate());
        dto.setEndDate(plan.getEndDate());
        dto.setStatus(plan.getStatus());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());
        PlanDTO.Schedule schedule = fromJsonSchedule(plan.getSchedule());
        dto.setItemCount(schedule.getItems() != null ? schedule.getItems().size() : 0);
        return dto;
    }

    private PlanDTO.Detail toDetail(Plan plan) {
        PlanDTO.Detail dto = new PlanDTO.Detail();
        dto.setId(plan.getId());
        dto.setName(plan.getName());
        dto.setPlanType(plan.getPlanType());
        dto.setStartDate(plan.getStartDate());
        dto.setEndDate(plan.getEndDate());
        dto.setStatus(plan.getStatus());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt());

        PlanDTO.Schedule schedule = fromJsonSchedule(plan.getSchedule());
        List<PlanDTO.ScheduleItemDetail> items = new ArrayList<>();
        if (schedule.getItems() != null) {
            Set<Long> programIds = schedule.getItems().stream()
                    .map(PlanDTO.ScheduleItem::getProgramId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, Program> programMap = programRepository.findAllById(programIds).stream()
                    .collect(Collectors.toMap(Program::getId, Function.identity()));

            for (PlanDTO.ScheduleItem item : schedule.getItems()) {
                PlanDTO.ScheduleItemDetail detail = new PlanDTO.ScheduleItemDetail();
                detail.setProgramId(item.getProgramId());
                detail.setDayOfWeek(item.getDayOfWeek());
                detail.setStartTime(item.getStartTime());
                detail.setEndTime(item.getEndTime());
                Program program = programMap.get(item.getProgramId());
                if (program != null) {
                    detail.setProgramName(program.getName());
                    detail.setPlayType(program.getPlayType());
                } else {
                    detail.setProgramName("栏目已删除");
                }
                items.add(detail);
            }
        }
        dto.setItems(items);
        return dto;
    }

    private String toJsonSchedule(PlanDTO.Schedule schedule) {
        try {
            return objectMapper.writeValueAsString(schedule);
        } catch (JsonProcessingException e) {
            throw new BizException("时间段配置序列化失败");
        }
    }

    private PlanDTO.Schedule fromJsonSchedule(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new PlanDTO.Schedule();
            }
            return objectMapper.readValue(json, PlanDTO.Schedule.class);
        } catch (JsonProcessingException e) {
            return new PlanDTO.Schedule();
        }
    }
}
