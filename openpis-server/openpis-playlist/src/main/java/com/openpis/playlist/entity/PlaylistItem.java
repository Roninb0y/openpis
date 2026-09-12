package com.openpis.playlist.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "playlist_item")
@Data
public class PlaylistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playlist_id", nullable = false)
    private Long playlistId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 该项播放时长（秒），NULL 表示使用媒体原时长
     */
    private Integer duration;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
