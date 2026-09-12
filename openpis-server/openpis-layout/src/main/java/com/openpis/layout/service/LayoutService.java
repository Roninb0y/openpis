package com.openpis.layout.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openpis.common.exception.BizException;
import com.openpis.layout.dto.LayoutDTO;
import com.openpis.layout.entity.Layout;
import com.openpis.layout.repository.LayoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LayoutService {

    private final LayoutRepository layoutRepository;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询布局列表
     */
    public Page<LayoutDTO.ListItem> list(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Layout> layouts;

        if (StringUtils.hasText(keyword)) {
            layouts = layoutRepository.findByNameContainingIgnoreCase(keyword, pageable);
        } else {
            layouts = layoutRepository.findAll(pageable);
        }

        return layouts.map(layout -> {
            LayoutDTO.ListItem dto = new LayoutDTO.ListItem();
            dto.setId(layout.getId());
            dto.setName(layout.getName());
            dto.setResolution(layout.getResolution());
            dto.setWidth(layout.getWidth());
            dto.setHeight(layout.getHeight());
            dto.setCreatedAt(layout.getCreatedAt());
            dto.setModuleCount(parseModules(layout.getModules()).size());
            return dto;
        });
    }

    /**
     * 创建布局
     */
    @Transactional
    public LayoutDTO.Detail create(LayoutDTO.CreateRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("布局名称不能为空");
        }
        if (request.getWidth() == null || request.getHeight() == null
                || request.getWidth() <= 0 || request.getHeight() <= 0) {
            throw new BizException("分辨率不合法");
        }

        Layout layout = new Layout();
        layout.setName(request.getName());
        layout.setResolution(StringUtils.hasText(request.getResolution())
                ? request.getResolution() : request.getWidth() + "x" + request.getHeight());
        layout.setWidth(request.getWidth());
        layout.setHeight(request.getHeight());
        layout.setModules(toJson(request.getModules() != null ? request.getModules() : Collections.emptyList()));
        layout = layoutRepository.save(layout);

        return toDetail(layout);
    }

    /**
     * 获取布局详情
     */
    public LayoutDTO.Detail getDetail(Long id) {
        Layout layout = layoutRepository.findById(id)
                .orElseThrow(() -> new BizException("布局不存在"));
        return toDetail(layout);
    }

    /**
     * 更新布局
     */
    @Transactional
    public LayoutDTO.Detail update(Long id, LayoutDTO.UpdateRequest request) {
        Layout layout = layoutRepository.findById(id)
                .orElseThrow(() -> new BizException("布局不存在"));

        if (StringUtils.hasText(request.getName())) {
            layout.setName(request.getName());
        }
        if (StringUtils.hasText(request.getResolution())) {
            layout.setResolution(request.getResolution());
        }
        if (request.getWidth() != null) {
            layout.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            layout.setHeight(request.getHeight());
        }
        if (request.getModules() != null) {
            layout.setModules(toJson(request.getModules()));
        }
        layoutRepository.save(layout);

        return toDetail(layout);
    }

    /**
     * 删除布局
     */
    @Transactional
    public void delete(Long id) {
        if (!layoutRepository.existsById(id)) {
            throw new BizException("布局不存在");
        }
        layoutRepository.deleteById(id);
    }

    private LayoutDTO.Detail toDetail(Layout layout) {
        LayoutDTO.Detail dto = new LayoutDTO.Detail();
        dto.setId(layout.getId());
        dto.setName(layout.getName());
        dto.setResolution(layout.getResolution());
        dto.setWidth(layout.getWidth());
        dto.setHeight(layout.getHeight());
        dto.setCreatedAt(layout.getCreatedAt());
        dto.setModules(parseModules(layout.getModules()));
        return dto;
    }

    private String toJson(List<LayoutDTO.ModuleConfig> modules) {
        try {
            return objectMapper.writeValueAsString(modules);
        } catch (JsonProcessingException e) {
            throw new BizException("模块配置序列化失败");
        }
    }

    private List<LayoutDTO.ModuleConfig> parseModules(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<LayoutDTO.ModuleConfig>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
