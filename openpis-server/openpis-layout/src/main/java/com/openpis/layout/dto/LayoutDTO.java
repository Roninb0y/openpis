package com.openpis.layout.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class LayoutDTO {

    @Data
    public static class ModuleConfig {
        private String id;
        private String name;
        private String type;    // video / image / text / flash
        private Double x;
        private Double y;
        private Double w;
        private Double h;
        /**
         * Lombok 生成的 getZIndex() 会被 Jackson 解析成 "zindex"，
         * 手写 getter/setter 并 @JsonIgnore，统一只暴露 "zIndex"
         */
        @JsonProperty("zIndex")
        private Integer zIndex;

        @JsonIgnore
        public Integer getZIndex() {
            return zIndex;
        }

        @JsonIgnore
        public void setZIndex(Integer zIndex) {
            this.zIndex = zIndex;
        }
    }

    @Data
    public static class CreateRequest {
        private String name;
        private String resolution;
        private Integer width;
        private Integer height;
        private List<ModuleConfig> modules;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private String resolution;
        private Integer width;
        private Integer height;
        private List<ModuleConfig> modules;
    }

    @Data
    public static class ListItem {
        private Long id;
        private String name;
        private String resolution;
        private Integer width;
        private Integer height;
        private Integer moduleCount;
        private LocalDateTime createdAt;
    }

    @Data
    public static class Detail {
        private Long id;
        private String name;
        private String resolution;
        private Integer width;
        private Integer height;
        private List<ModuleConfig> modules;
        private LocalDateTime createdAt;
    }
}
