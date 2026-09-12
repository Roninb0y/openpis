package com.openpis.playlist.controller;

import com.openpis.common.result.Result;
import com.openpis.playlist.dto.PlaylistDTO;
import com.openpis.playlist.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping
    public Result<Page<PlaylistDTO.ListResponse>> list(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String keyword) {
        return Result.success(playlistService.list(page, size, type, keyword));
    }

    @PostMapping
    public Result<PlaylistDTO.DetailResponse> create(@RequestBody PlaylistDTO.CreateRequest request) {
        return Result.success(playlistService.create(request));
    }

    @GetMapping("/{id}")
    public Result<PlaylistDTO.DetailResponse> detail(@PathVariable Long id) {
        return Result.success(playlistService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<PlaylistDTO.DetailResponse> update(@PathVariable Long id,
                                                     @RequestBody PlaylistDTO.UpdateRequest request) {
        return Result.success(playlistService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playlistService.delete(id);
        return Result.success();
    }
}
