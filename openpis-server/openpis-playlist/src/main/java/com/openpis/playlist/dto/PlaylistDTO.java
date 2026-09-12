package com.openpis.playlist.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class PlaylistDTO {

    // ========== 请求 ==========

    @Data
    public static class CreateRequest {
        private String name;
        private String type;
        private List<ItemRequest> items;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private String type;
        private List<ItemRequest> items;
    }

    @Data
    public static class ItemRequest {
        private Long materialId;
        private Integer sortOrder;
        private Integer duration;
    }

    // ========== 响应 ==========

    @Data
    public static class ListResponse {
        private Long id;
        private String name;
        private String type;
        private Integer itemCount;
        private LocalDateTime createdAt;
    }

    @Data
    public static class DetailResponse {
        private Long id;
        private String name;
        private String type;
        private List<ItemDetail> items;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ItemDetail {
        private Long id;
        private Long materialId;
        private String materialTitle;
        private String materialType;
        private String materialFilePath;
        private String materialThumbnail;
        private Integer materialDuration;
        private Integer sortOrder;
        private Integer duration;
    }
}
