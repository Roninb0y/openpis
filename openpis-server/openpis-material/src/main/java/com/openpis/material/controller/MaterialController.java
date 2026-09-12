package com.openpis.material.controller;

import com.openpis.common.result.Result;
import com.openpis.material.dto.MaterialUpdateRequest;
import com.openpis.material.entity.Material;
import com.openpis.material.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping("/upload")
    public Result<Material> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "description", required = false) String description,
                                   @RequestParam(value = "duration", required = false) Integer duration) {
        return Result.success(materialService.upload(file, description, duration));
    }

    @GetMapping
    public Result<Page<Material>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String keyword) {
        return Result.success(materialService.list(page, size, type, keyword));
    }

    @GetMapping("/{id}")
    public Result<Material> detail(@PathVariable Long id) {
        return materialService.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "媒体不存在"));
    }

    @PutMapping("/{id}")
    public Result<Material> update(@PathVariable Long id, @RequestBody MaterialUpdateRequest request) {
        return Result.success(materialService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.success();
    }
}
