package com.openpis.layout.controller;

import com.openpis.common.result.Result;
import com.openpis.layout.dto.LayoutDTO;
import com.openpis.layout.service.LayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/layouts")
@RequiredArgsConstructor
public class LayoutController {

    private final LayoutService layoutService;

    @GetMapping
    public Result<Page<LayoutDTO.ListItem>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String keyword) {
        return Result.success(layoutService.list(page, size, keyword));
    }

    @PostMapping
    public Result<LayoutDTO.Detail> create(@RequestBody LayoutDTO.CreateRequest request) {
        return Result.success(layoutService.create(request));
    }

    @GetMapping("/{id}")
    public Result<LayoutDTO.Detail> detail(@PathVariable Long id) {
        return Result.success(layoutService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<LayoutDTO.Detail> update(@PathVariable Long id,
                                           @RequestBody LayoutDTO.UpdateRequest request) {
        return Result.success(layoutService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        layoutService.delete(id);
        return Result.success();
    }
}
