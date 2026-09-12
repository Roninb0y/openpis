package com.openpis.program.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class ProgramDTO {

    // ========== 请求 ==========

    @Data
    public static class BindingRequest {
        private String moduleId;
        private Long playlistId;
    }

    @Data
    public static class CreateRequest {
        private String name;
        private Long layoutId;
        private String playType;
        private List<BindingRequest> modulePlaylists;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private Long layoutId;
        private String playType;
        private List<BindingRequest> modulePlaylists;
    }

    // ========== 响应 ==========

    @Data
    public static class ListItem {
        private Long id;
        private String name;
        private Long layoutId;
        private String layoutName;
        private String playType;
        private Integer moduleCount;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ModuleBinding {
        private String moduleId;
        private String moduleName;
        private String moduleType;
        private Long playlistId;
        private String playlistName;
        private String playlistType;
        private Integer itemCount;
    }

    @Data
    public static class Detail {
        private Long id;
        private String name;
        private Long layoutId;
        private String layoutName;
        private String playType;
        private Integer width;
        private Integer height;
        private List<ModuleBinding> modules;
        private LocalDateTime createdAt;
    }
}
