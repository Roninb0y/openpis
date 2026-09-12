package com.openpis.player.controller;

import com.openpis.common.result.Result;
import com.openpis.player.dto.PlayerDTO;
import com.openpis.player.entity.Device;
import com.openpis.player.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlayerController {

    private final PlayerService playerService;

    /**
     * 播放端获取当前应播计划。
     * @param code 设备编码（可选，用于心跳）
     * @param at   模拟时间（可选，ISO 格式 yyyy-MM-dd'T'HH:mm:ss，便于测试跨时间段切换）
     */
    @GetMapping("/player/current")
    public Result<PlayerDTO.CurrentResponse> current(@RequestParam(required = false) String code,
                                                     @RequestParam(required = false) String at) {
        LocalDateTime now = LocalDateTime.now();
        if (at != null && !at.isBlank()) {
            try {
                now = LocalDateTime.parse(at.trim());
            } catch (Exception ignored) {
            }
        }
        return Result.success(playerService.resolveCurrent(code, now));
    }

    /**
     * 播放端批量上报播放日志
     */
    @PostMapping("/player/log")
    public Result<Void> reportLogs(@RequestBody List<PlayerDTO.PlayLogItem> logs) {
        playerService.reportLogs(logs);
        return Result.success();
    }

    /**
     * 设备心跳（MVP 仅更新心跳字段）
     */
    @PostMapping("/device/heartbeat")
    public Result<Long> heartbeat(@RequestBody PlayerDTO.HeartbeatRequest request) {
        Device device = playerService.heartbeat(request.getCode(), request.getName());
        return Result.success(device.getId());
    }
}
