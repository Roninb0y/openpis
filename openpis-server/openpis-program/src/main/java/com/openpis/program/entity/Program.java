package com.openpis.program.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "program")
@Data
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "layout_id", nullable = false)
    private Long layoutId;

    /**
     * 播放类型：loop 垫播 / insert 插播 / emergency 紧急
     */
    @Column(name = "play_type", nullable = false, length = 32)
    private String playType;

    /**
     * 模块绑定播表 JSON：[{"moduleId":"m1","playlistId":3},...]
     */
    @Column(name = "module_playlists", columnDefinition = "JSON")
    private String modulePlaylists;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
