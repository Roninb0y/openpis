package com.openpis.plan.controller;

import com.openpis.common.result.Result;
import com.openpis.plan.dto.PlanDTO;
import com.openpis.plan.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public Result<Page<PlanDTO.ListItem>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(planService.list(page, size, status, keyword));
    }

    @PostMapping
    public Result<PlanDTO.Detail> create(@RequestBody PlanDTO.CreateRequest request) {
        return Result.success(planService.create(request));
    }

    @GetMapping("/{id}")
    public Result<PlanDTO.Detail> detail(@PathVariable Long id) {
        return Result.success(planService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<PlanDTO.Detail> update(@PathVariable Long id,
                                         @RequestBody PlanDTO.UpdateRequest request) {
        return Result.success(planService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        planService.publish(id);
        return Result.success();
    }

    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        planService.unpublish(id);
        return Result.success();
    }
}
