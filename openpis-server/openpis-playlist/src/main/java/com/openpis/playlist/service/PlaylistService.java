package com.openpis.playlist.service;

import com.openpis.common.exception.BizException;
import com.openpis.material.entity.Material;
import com.openpis.material.repository.MaterialRepository;
import com.openpis.playlist.dto.PlaylistDTO;
import com.openpis.playlist.entity.Playlist;
import com.openpis.playlist.entity.PlaylistItem;
import com.openpis.playlist.repository.PlaylistItemRepository;
import com.openpis.playlist.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final MaterialRepository materialRepository;

    /**
     * 分页查询播表列表
     */
    public Page<PlaylistDTO.ListResponse> list(int page, int size, String type, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Playlist> playlists;

        boolean hasType = StringUtils.hasText(type);
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasType && hasKeyword) {
            playlists = playlistRepository.findByNameContainingIgnoreCaseAndType(keyword, type, pageable);
        } else if (hasType) {
            playlists = playlistRepository.findByType(type, pageable);
        } else if (hasKeyword) {
            playlists = playlistRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            playlists = playlistRepository.findAll(pageable);
        }

        return playlists.map(playlist -> {
            PlaylistDTO.ListResponse dto = new PlaylistDTO.ListResponse();
            dto.setId(playlist.getId());
            dto.setName(playlist.getName());
            dto.setType(playlist.getType());
            dto.setItemCount(playlistItemRepository.findByPlaylistIdOrderBySortOrderAsc(playlist.getId()).size());
            dto.setCreatedAt(playlist.getCreatedAt());
            return dto;
        });
    }

    /**
     * 创建播表
     */
    @Transactional
    public PlaylistDTO.DetailResponse create(PlaylistDTO.CreateRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("播表名称不能为空");
        }
        String type = StringUtils.hasText(request.getType()) ? request.getType() : "video";
        if (!Set.of("video", "image", "text").contains(type)) {
            throw new BizException("播表类型不合法");
        }

        Playlist playlist = new Playlist();
        playlist.setName(request.getName());
        playlist.setType(type);
        playlist = playlistRepository.save(playlist);

        saveItems(playlist.getId(), request.getItems());
        return getDetail(playlist.getId());
    }

    /**
     * 获取播表详情（含播表项 + 媒体信息，批量查询避免 N+1）
     */
    public PlaylistDTO.DetailResponse getDetail(Long id) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BizException("播表不存在"));

        List<PlaylistItem> items = playlistItemRepository.findByPlaylistIdOrderBySortOrderAsc(id);

        List<Long> materialIds = items.stream()
                .map(PlaylistItem::getMaterialId)
                .collect(Collectors.toList());
        Map<Long, Material> materialMap = materialRepository.findAllById(materialIds).stream()
                .collect(Collectors.toMap(Material::getId, m -> m));

        PlaylistDTO.DetailResponse dto = new PlaylistDTO.DetailResponse();
        dto.setId(playlist.getId());
        dto.setName(playlist.getName());
        dto.setType(playlist.getType());
        dto.setCreatedAt(playlist.getCreatedAt());

        List<PlaylistDTO.ItemDetail> itemDetails = items.stream().map(item -> {
            PlaylistDTO.ItemDetail detail = new PlaylistDTO.ItemDetail();
            detail.setId(item.getId());
            detail.setMaterialId(item.getMaterialId());
            detail.setSortOrder(item.getSortOrder());
            detail.setDuration(item.getDuration());

            Material material = materialMap.get(item.getMaterialId());
            if (material != null) {
                detail.setMaterialTitle(material.getTitle());
                detail.setMaterialType(material.getType());
                detail.setMaterialFilePath(material.getFilePath());
                detail.setMaterialThumbnail(material.getThumbnail());
                detail.setMaterialDuration(material.getDuration());
            }
            return detail;
        }).collect(Collectors.toList());

        dto.setItems(itemDetails);
        return dto;
    }

    /**
     * 更新播表（播表项全量替换：先删后增）
     */
    @Transactional
    public PlaylistDTO.DetailResponse update(Long id, PlaylistDTO.UpdateRequest request) {
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new BizException("播表不存在"));

        if (StringUtils.hasText(request.getName())) {
            playlist.setName(request.getName());
        }
        if (StringUtils.hasText(request.getType())) {
            if (!Set.of("video", "image", "text").contains(request.getType())) {
                throw new BizException("播表类型不合法");
            }
            playlist.setType(request.getType());
        }
        playlistRepository.save(playlist);

        if (request.getItems() != null) {
            playlistItemRepository.deleteByPlaylistId(id);
            saveItems(id, request.getItems());
        }

        return getDetail(id);
    }

    /**
     * 删除播表
     */
    @Transactional
    public void delete(Long id) {
        if (!playlistRepository.existsById(id)) {
            throw new BizException("播表不存在");
        }
        playlistItemRepository.deleteByPlaylistId(id);
        playlistRepository.deleteById(id);
    }

    private void saveItems(Long playlistId, List<PlaylistDTO.ItemRequest> itemRequests) {
        if (itemRequests == null || itemRequests.isEmpty()) {
            return;
        }
        List<PlaylistItem> items = new ArrayList<>();
        int order = 0;
        for (PlaylistDTO.ItemRequest itemReq : itemRequests) {
            if (itemReq.getMaterialId() == null) {
                continue;
            }
            PlaylistItem item = new PlaylistItem();
            item.setPlaylistId(playlistId);
            item.setMaterialId(itemReq.getMaterialId());
            item.setSortOrder(itemReq.getSortOrder() != null ? itemReq.getSortOrder() : order);
            item.setDuration(itemReq.getDuration());
            items.add(item);
            order++;
        }
        playlistItemRepository.saveAll(items);
    }
}
