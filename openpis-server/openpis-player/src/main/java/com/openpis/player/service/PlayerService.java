package com.openpis.player.service;

import com.openpis.common.config.FileStorageConfig;
import com.openpis.common.exception.BizException;
import com.openpis.layout.dto.LayoutDTO;
import com.openpis.layout.service.LayoutService;
import com.openpis.plan.dto.PlanDTO;
import com.openpis.plan.entity.Plan;
import com.openpis.plan.repository.PlanRepository;
import com.openpis.player.dto.PlayerDTO;
import com.openpis.player.entity.Device;
import com.openpis.player.entity.PlayLog;
import com.openpis.player.repository.DeviceRepository;
import com.openpis.player.repository.PlayLogRepository;
import com.openpis.playlist.dto.PlaylistDTO;
import com.openpis.playlist.service.PlaylistService;
import com.openpis.program.dto.ProgramDTO;
import com.openpis.program.entity.Program;
import com.openpis.program.repository.ProgramRepository;
import com.openpis.program.service.ProgramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 播放端核心服务：
 * 1. 设备心跳（MVP 只更新心跳字段，不做设备管理页面）
 * 2. 播放日志上报
 * 3. "当前应播计划"解析 —— 根据当前时间在已发布计划中选出应播栏目及其完整播放数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final DeviceRepository deviceRepository;
    private final PlayLogRepository playLogRepository;
    private final PlanRepository planRepository;
    private final ProgramRepository programRepository;
    private final ProgramService programService;
    private final LayoutService layoutService;
    private final PlaylistService playlistService;
    private final FileStorageConfig fileStorageConfig;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 播放类型优先级：数值越小越优先。同一时间多计划重叠时，紧急 > 插播 > 垫播
     */
    private static int priorityOf(String playType) {
        return switch (playType) {
            case "emergency" -> 0;
            case "insert" -> 1;
            default -> 2;
        };
    }

    // ========== 设备心跳 ==========

    @Transactional
    public Device heartbeat(String code, String name) {
        if (!StringUtils.hasText(code)) {
            throw new BizException("设备编码不能为空");
        }
        Device device = deviceRepository.findByCode(code).orElseGet(() -> {
            Device d = new Device();
            d.setCode(code);
            d.setName(StringUtils.hasText(name) ? name : "终端-" + code);
            return d;
        });
        device.setStatus("online");
        device.setLastHeartbeat(LocalDateTime.now());
        if (StringUtils.hasText(name)) {
            device.setName(name);
        }
        return deviceRepository.save(device);
    }

    // ========== 播放日志 ==========

    @Transactional
    public void reportLogs(List<PlayerDTO.PlayLogItem> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        List<PlayLog> entities = new ArrayList<>();
        for (PlayerDTO.PlayLogItem item : logs) {
            if (item.getMaterialId() == null) {
                continue;
            }
            Device device = StringUtils.hasText(item.getDeviceCode())
                    ? heartbeat(item.getDeviceCode(), null) : null;

            PlayLog logEntity = new PlayLog();
            logEntity.setDeviceId(device != null ? device.getId() : null);
            logEntity.setProgramId(item.getProgramId());
            logEntity.setMaterialId(item.getMaterialId());
            logEntity.setStartedAt(parseDateTime(item.getStartedAt()));
            logEntity.setEndedAt(parseDateTime(item.getEndedAt()));
            entities.add(logEntity);
        }
        playLogRepository.saveAll(entities);
    }

    // ========== 当前应播计划解析 ==========

    /**
     * 解析当前时间应播放的计划：
     * 1. 找出生效期覆盖今天、且已发布的计划
     * 2. 日计划每天生效；周计划按 dayOfWeek（1=周一 ... 7=周日）过滤
     * 3. 时间段命中 [startTime, endTime)，支持跨零点
     * 4. 多个命中时按播放类型优先级取最高：emergency > insert > loop，
     *    同优先级取更新时间较新（其次 id 较大）的计划
     */
    public PlayerDTO.CurrentResponse resolveCurrent(String code, LocalDateTime now) {
        if (StringUtils.hasText(code)) {
            try {
                heartbeat(code, null);
            } catch (Exception e) {
                log.warn("播放端心跳更新失败: {}", e.getMessage());
            }
        }

        LocalDate today = now.toLocalDate();
        int dayOfWeek = now.getDayOfWeek().getValue();
        LocalTime time = now.toLocalTime();

        List<Plan> plans = planRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                1, today, today);

        Plan bestPlan = null;
        Program bestProgram = null;
        for (Plan plan : plans) {
            PlanDTO.Schedule schedule = fromJsonSchedule(plan.getSchedule());
            if (schedule.getItems() == null) {
                continue;
            }
            for (PlanDTO.ScheduleItem item : schedule.getItems()) {
                if (!"daily".equals(plan.getPlanType())) {
                    if (item.getDayOfWeek() == null || item.getDayOfWeek() != dayOfWeek) {
                        continue;
                    }
                }
                LocalTime start = parseTime(item.getStartTime());
                LocalTime end = parseTime(item.getEndTime());
                if (start == null || end == null) {
                    continue;
                }
                boolean crossMidnight = !end.isAfter(start);
                boolean inRange = crossMidnight
                        ? (!time.isBefore(start) || time.isBefore(end))
                        : (!time.isBefore(start) && time.isBefore(end));
                if (!inRange) {
                    continue;
                }

                Optional<Program> programOpt = programRepository.findById(item.getProgramId());
                if (programOpt.isEmpty()) {
                    continue;
                }
                Program program = programOpt.get();

                if (bestPlan == null || betterThan(program, plan, bestProgram, bestPlan)) {
                    bestPlan = plan;
                    bestProgram = program;
                }
            }
        }

        if (bestPlan == null) {
            return null;
        }
        return buildResponse(bestPlan, bestProgram);
    }

    private boolean betterThan(Program candidate, Plan candidatePlan, Program current, Plan currentPlan) {
        int candidatePriority = priorityOf(candidate.getPlayType());
        int currentPriority = priorityOf(current.getPlayType());
        if (candidatePriority != currentPriority) {
            return candidatePriority < currentPriority;
        }
        // 同优先级：更新时间较新者优先，再比较 id
        var candidateUpdated = candidatePlan.getUpdatedAt();
        var currentUpdated = currentPlan.getUpdatedAt();
        if (candidateUpdated != null && currentUpdated != null && !candidateUpdated.equals(currentUpdated)) {
            return candidateUpdated.isAfter(currentUpdated);
        }
        if (candidateUpdated == null && currentUpdated != null) {
            return false;
        }
        if (candidateUpdated != null && currentUpdated == null) {
            return true;
        }
        return candidatePlan.getId() > currentPlan.getId();
    }

    private PlayerDTO.CurrentResponse buildResponse(Plan plan, Program program) {
        PlayerDTO.CurrentResponse response = new PlayerDTO.CurrentResponse();
        response.setPlanId(plan.getId());
        response.setPlanName(plan.getName());
        response.setProgramId(program.getId());
        response.setProgramName(program.getName());
        response.setPlayType(program.getPlayType());

        // 布局信息
        LayoutDTO.Detail layout = layoutService.getDetail(program.getLayoutId());
        response.setLayoutWidth(layout.getWidth());
        response.setLayoutHeight(layout.getHeight());

        // 模块绑定播表
        Map<String, Long> bindings = programService.getDetail(program.getId()).getModules().stream()
                .filter(mb -> mb.getPlaylistId() != null)
                .collect(Collectors.toMap(ProgramDTO.ModuleBinding::getModuleId, ProgramDTO.ModuleBinding::getPlaylistId,
                        (a, b) -> b));

        List<PlayerDTO.ModuleVO> modules = new ArrayList<>();
        List<LayoutDTO.ModuleConfig> layoutModules = new ArrayList<>(layout.getModules());
        layoutModules.sort(Comparator.comparingInt(m -> m.getZIndex() == null ? 0 : m.getZIndex()));

        for (LayoutDTO.ModuleConfig module : layoutModules) {
            PlayerDTO.ModuleVO moduleVO = new PlayerDTO.ModuleVO();
            moduleVO.setModuleId(module.getId());
            moduleVO.setName(module.getName());
            moduleVO.setType(module.getType());
            moduleVO.setX(module.getX());
            moduleVO.setY(module.getY());
            moduleVO.setW(module.getW());
            moduleVO.setH(module.getH());
            moduleVO.setZIndex(module.getZIndex());

            Long playlistId = bindings.get(module.getId());
            if (playlistId != null) {
                moduleVO.setPlaylist(buildPlaylistVO(playlistId));
            }
            modules.add(moduleVO);
        }
        response.setModules(modules);
        return response;
    }

    private PlayerDTO.PlaylistVO buildPlaylistVO(Long playlistId) {
        try {
            PlaylistDTO.DetailResponse detail = playlistService.getDetail(playlistId);
            PlayerDTO.PlaylistVO playlistVO = new PlayerDTO.PlaylistVO();
            playlistVO.setId(detail.getId());
            playlistVO.setName(detail.getName());
            playlistVO.setType(detail.getType());

            List<PlayerDTO.PlaylistItemVO> items = detail.getItems().stream()
                    .filter(item -> StringUtils.hasText(item.getMaterialFilePath()))
                    .map(item -> {
                        PlayerDTO.PlaylistItemVO vo = new PlayerDTO.PlaylistItemVO();
                        vo.setMaterialId(item.getMaterialId());
                        vo.setTitle(item.getMaterialTitle());
                        vo.setMaterialType(item.getMaterialType());
                        vo.setUrl(fileStorageConfig.fileUrl(item.getMaterialFilePath()));
                        vo.setDuration(effectiveDuration(item));
                        return vo;
                    })
                    .collect(Collectors.toList());
            playlistVO.setItems(items);
            return playlistVO;
        } catch (Exception e) {
            log.warn("加载播表 {} 失败: {}", playlistId, e.getMessage());
            return null;
        }
    }

    private Integer effectiveDuration(PlaylistDTO.ItemDetail item) {
        if (item.getDuration() != null && item.getDuration() > 0) {
            return item.getDuration();
        }
        if (item.getMaterialDuration() != null && item.getMaterialDuration() > 0) {
            return item.getMaterialDuration();
        }
        String type = item.getMaterialType();
        if ("image".equals(type)) {
            return 10;
        }
        if ("text".equals(type)) {
            return 30;
        }
        return 0; // 视频按自然播完处理
    }

    // ========== 工具方法 ==========

    private PlanDTO.Schedule fromJsonSchedule(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return new PlanDTO.Schedule();
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, PlanDTO.Schedule.class);
        } catch (Exception e) {
            return new PlanDTO.Schedule();
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

    private LocalDateTime parseDateTime(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String v = s.trim();
        try {
            return LocalDateTime.parse(v, DATETIME_FORMAT);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(v);
        } catch (Exception ignored) {
        }
        try {
            return Instant.parse(v).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception ignored) {
        }
        return null;
    }
}
