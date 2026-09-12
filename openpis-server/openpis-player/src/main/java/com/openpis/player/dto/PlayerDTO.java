package com.openpis.player.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

public class PlayerDTO {

    @Data
    public static class PlaylistItemVO {
        private Long materialId;
        private String title;
        private String materialType;
        private String url;
        /**
         * 建议播放时长（秒）：播表项时长 > 媒体时长 > 类型默认值（图片10/字幕30，视频为0由播放器按自然播完处理）
         */
        private Integer duration;
    }

    @Data
    public static class PlaylistVO {
        private Long id;
        private String name;
        private String type;
        private List<PlaylistItemVO> items;
    }

    @Data
    public static class ModuleVO {
        private String moduleId;
        private String name;
        private String type;
        private Double x;
        private Double y;
        private Double w;
        private Double h;
        /**
         * 同 LayoutDTO.ModuleConfig：统一只暴露 "zIndex" 属性
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

        private PlaylistVO playlist;
    }

    @Data
    public static class CurrentResponse {
        private Long planId;
        private String planName;
        private Long programId;
        private String programName;
        private String playType;
        private Integer layoutWidth;
        private Integer layoutHeight;
        private List<ModuleVO> modules;
    }

    @Data
    public static class HeartbeatRequest {
        private String code;
        private String name;
    }

    @Data
    public static class PlayLogItem {
        private String deviceCode;
        private Long programId;
        private Long materialId;
        private String startedAt;
        private String endedAt;
    }
}
