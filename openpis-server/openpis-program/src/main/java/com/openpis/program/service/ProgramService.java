package com.openpis.program.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openpis.common.exception.BizException;
import com.openpis.layout.dto.LayoutDTO;
import com.openpis.layout.entity.Layout;
import com.openpis.layout.repository.LayoutRepository;
import com.openpis.layout.service.LayoutService;
import com.openpis.playlist.entity.Playlist;
import com.openpis.playlist.repository.PlaylistItemRepository;
import com.openpis.playlist.repository.PlaylistRepository;
import com.openpis.program.dto.ProgramDTO;
import com.openpis.program.entity.Program;
import com.openpis.program.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgramService {

    private final ProgramRepository programRepository;
    private final LayoutRepository layoutRepository;
    private final LayoutService layoutService;
    private final PlaylistRepository playlistRepository;
    private final PlaylistItemRepository playlistItemRepository;
    private final ObjectMapper objectMapper;

    private static final Set<String> PLAY_TYPES = Set.of("loop", "insert", "emergency");

    /**
     * 模块媒体类型 → 允许绑定的播表类型
     * video 视频模块 → video 播表（可混排图片）
     * image 图片模块 → image 播表
     * text  字幕模块 → text 播表
     * flash 模块    → image 播表（MVP 中 flash 作为占位，用图片播表兜底）
     */
    private static final Map<String, String> MODULE_TYPE_TO_PLAYLIST_TYPE = Map.of(
            "video", "video",
            "image", "image",
            "text", "text",
            "flash", "image"
    );

    /**
     * 分页查询栏目列表
     */
    public Page<ProgramDTO.ListItem> list(int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Program> programs = StringUtils.hasText(keyword)
                ? programRepository.findByNameContainingIgnoreCase(keyword, pageable)
                : programRepository.findAll(pageable);

        Set<Long> layoutIds = programs.getContent().stream()
                .map(Program::getLayoutId)
                .collect(Collectors.toSet());
        Map<Long, Layout> layoutMap = layoutRepository.findAllById(layoutIds).stream()
                .collect(Collectors.toMap(Layout::getId, Function.identity()));

        List<ProgramDTO.ListItem> items = programs.getContent().stream().map(program -> {
            ProgramDTO.ListItem dto = new ProgramDTO.ListItem();
            dto.setId(program.getId());
            dto.setName(program.getName());
            dto.setLayoutId(program.getLayoutId());
            Layout layout = layoutMap.get(program.getLayoutId());
            dto.setLayoutName(layout != null ? layout.getName() : "布局已删除");
            dto.setPlayType(program.getPlayType());
            dto.setModuleCount(parseBindings(program.getModulePlaylists()).size());
            dto.setCreatedAt(program.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(items, pageable, programs.getTotalElements());
    }

    /**
     * 创建栏目
     */
    @Transactional
    public ProgramDTO.Detail create(ProgramDTO.CreateRequest request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("栏目名称不能为空");
        }
        if (request.getLayoutId() == null || !layoutRepository.existsById(request.getLayoutId())) {
            throw new BizException("布局不存在");
        }
        String playType = StringUtils.hasText(request.getPlayType()) ? request.getPlayType() : "loop";
        if (!PLAY_TYPES.contains(playType)) {
            throw new BizException("播放类型不合法");
        }
        validateBindings(request.getLayoutId(), request.getModulePlaylists());

        Program program = new Program();
        program.setName(request.getName());
        program.setLayoutId(request.getLayoutId());
        program.setPlayType(playType);
        program.setModulePlaylists(toJson(request.getModulePlaylists()));
        program = programRepository.save(program);

        return getDetail(program.getId());
    }

    /**
     * 获取栏目详情
     */
    public ProgramDTO.Detail getDetail(Long id) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new BizException("栏目不存在"));
        return toDetail(program);
    }

    /**
     * 更新栏目
     */
    @Transactional
    public ProgramDTO.Detail update(Long id, ProgramDTO.UpdateRequest request) {
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new BizException("栏目不存在"));

        if (StringUtils.hasText(request.getName())) {
            program.setName(request.getName());
        }
        if (request.getLayoutId() != null) {
            if (!layoutRepository.existsById(request.getLayoutId())) {
                throw new BizException("布局不存在");
            }
            program.setLayoutId(request.getLayoutId());
        }
        if (StringUtils.hasText(request.getPlayType())) {
            if (!PLAY_TYPES.contains(request.getPlayType())) {
                throw new BizException("播放类型不合法");
            }
            program.setPlayType(request.getPlayType());
        }
        if (request.getModulePlaylists() != null) {
            validateBindings(program.getLayoutId(), request.getModulePlaylists());
            program.setModulePlaylists(toJson(request.getModulePlaylists()));
        }
        programRepository.save(program);

        return getDetail(id);
    }

    /**
     * 删除栏目
     */
    @Transactional
    public void delete(Long id) {
        if (!programRepository.existsById(id)) {
            throw new BizException("栏目不存在");
        }
        programRepository.deleteById(id);
    }

    // ========== 私有方法 ==========

    private void validateBindings(Long layoutId, List<ProgramDTO.BindingRequest> bindings) {
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        List<LayoutDTO.ModuleConfig> modules = layoutService.getDetail(layoutId).getModules();
        Map<String, LayoutDTO.ModuleConfig> moduleMap = modules.stream()
                .collect(Collectors.toMap(LayoutDTO.ModuleConfig::getId, Function.identity(), (a, b) -> a));

        for (ProgramDTO.BindingRequest binding : bindings) {
            if (!StringUtils.hasText(binding.getModuleId())) {
                continue;
            }
            LayoutDTO.ModuleConfig module = moduleMap.get(binding.getModuleId());
            if (module == null) {
                throw new BizException("模块 " + binding.getModuleId() + " 不存在于所选布局");
            }
            if (binding.getPlaylistId() == null) {
                continue;
            }
            Playlist playlist = playlistRepository.findById(binding.getPlaylistId())
                    .orElseThrow(() -> new BizException("绑定的播表不存在: " + binding.getPlaylistId()));

            String allowed = MODULE_TYPE_TO_PLAYLIST_TYPE.getOrDefault(module.getType(), "image");
            if (!allowed.equals(playlist.getType())) {
                throw new BizException(String.format(
                        "模块「%s」（%s）只能绑定 %s 类型的播表",
                        module.getName(), typeLabel(module.getType()), playlistTypeLabel(allowed)));
            }
        }
    }

    private ProgramDTO.Detail toDetail(Program program) {
        ProgramDTO.Detail detail = new ProgramDTO.Detail();
        detail.setId(program.getId());
        detail.setName(program.getName());
        detail.setLayoutId(program.getLayoutId());
        detail.setPlayType(program.getPlayType());
        detail.setCreatedAt(program.getCreatedAt());

        Layout layout = layoutRepository.findById(program.getLayoutId()).orElse(null);
        if (layout != null) {
            detail.setLayoutName(layout.getName());
            detail.setWidth(layout.getWidth());
            detail.setHeight(layout.getHeight());
        } else {
            detail.setLayoutName("布局已删除");
        }

        Map<String, Long> bindingMap = new HashMap<>();
        for (ProgramDTO.BindingRequest b : parseBindings(program.getModulePlaylists())) {
            if (StringUtils.hasText(b.getModuleId()) && b.getPlaylistId() != null) {
                bindingMap.put(b.getModuleId(), b.getPlaylistId());
            }
        }

        List<ProgramDTO.ModuleBinding> moduleBindings = new ArrayList<>();
        if (layout != null) {
            List<LayoutDTO.ModuleConfig> modules = layoutService.getDetail(layout.getId()).getModules();
            modules.sort(Comparator.comparingInt(m -> m.getZIndex() == null ? 0 : m.getZIndex()));
            for (LayoutDTO.ModuleConfig module : modules) {
                ProgramDTO.ModuleBinding mb = new ProgramDTO.ModuleBinding();
                mb.setModuleId(module.getId());
                mb.setModuleName(module.getName());
                mb.setModuleType(module.getType());

                Long playlistId = bindingMap.get(module.getId());
                if (playlistId != null) {
                    Optional<Playlist> playlist = playlistRepository.findById(playlistId);
                    playlist.ifPresent(p -> {
                        mb.setPlaylistId(p.getId());
                        mb.setPlaylistName(p.getName());
                        mb.setPlaylistType(p.getType());
                        mb.setItemCount(playlistItemRepository.findByPlaylistIdOrderBySortOrderAsc(p.getId()).size());
                    });
                }
                moduleBindings.add(mb);
            }
        }
        detail.setModules(moduleBindings);
        return detail;
    }

    private List<ProgramDTO.BindingRequest> parseBindings(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<ProgramDTO.BindingRequest>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private String toJson(List<ProgramDTO.BindingRequest> bindings) {
        try {
            return objectMapper.writeValueAsString(bindings != null ? bindings : Collections.emptyList());
        } catch (JsonProcessingException e) {
            throw new BizException("模块绑定序列化失败");
        }
    }

    private String typeLabel(String type) {
        return switch (type) {
            case "video" -> "视频";
            case "image" -> "图片";
            case "text" -> "字幕";
            case "flash" -> "Flash";
            default -> type;
        };
    }

    private String playlistTypeLabel(String type) {
        return switch (type) {
            case "video" -> "视频";
            case "image" -> "图片";
            case "text" -> "字幕";
            default -> type;
        };
    }
}
