package com.openpis.program.controller;

import com.openpis.common.result.Result;
import com.openpis.program.dto.ProgramDTO;
import com.openpis.program.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @GetMapping
    public Result<Page<ProgramDTO.ListItem>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String keyword) {
        return Result.success(programService.list(page, size, keyword));
    }

    @PostMapping
    public Result<ProgramDTO.Detail> create(@RequestBody ProgramDTO.CreateRequest request) {
        return Result.success(programService.create(request));
    }

    @GetMapping("/{id}")
    public Result<ProgramDTO.Detail> detail(@PathVariable Long id) {
        return Result.success(programService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<ProgramDTO.Detail> update(@PathVariable Long id,
                                            @RequestBody ProgramDTO.UpdateRequest request) {
        return Result.success(programService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        programService.delete(id);
        return Result.success();
    }
}
