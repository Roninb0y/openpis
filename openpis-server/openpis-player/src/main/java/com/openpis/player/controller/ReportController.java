package com.openpis.player.controller;

import com.openpis.common.result.Result;
import com.openpis.player.repository.PlayLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final PlayLogRepository playLogRepository;

    /**
     * 按日播放次数统计（缺数据的日期补 0）
     */
    @GetMapping("/daily")
    public Result<List<Map<String, Object>>> daily(@RequestParam(defaultValue = "7") int days) {
        int actualDays = Math.min(Math.max(days, 1), 90);
        LocalDate today = LocalDate.now();
        LocalDateTime since = today.minusDays(actualDays - 1L).atStartOfDay();

        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Object[] row : playLogRepository.countDaily(since)) {
            countMap.put(String.valueOf(row[0]), (Long) row[1]);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < actualDays; i++) {
            LocalDate date = today.minusDays(actualDays - 1L - i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("count", countMap.getOrDefault(date.toString(), 0L));
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 按栏目聚合播放次数
     */
    @GetMapping("/program")
    public Result<List<Map<String, Object>>> byProgram() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : playLogRepository.countByProgram()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("programId", row[0]);
            item.put("name", row[1]);
            item.put("count", row[2]);
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 按素材聚合播放次数（TOP N）
     */
    @GetMapping("/material")
    public Result<List<Map<String, Object>>> byMaterial(@RequestParam(defaultValue = "10") int limit) {
        int actualLimit = Math.min(Math.max(limit, 1), 100);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : playLogRepository.countByMaterial(actualLimit)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", row[0]);
            item.put("title", row[1]);
            item.put("count", row[2]);
            result.add(item);
        }
        return Result.success(result);
    }
}
