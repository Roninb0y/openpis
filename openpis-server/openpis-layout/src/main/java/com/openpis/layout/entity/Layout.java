package com.openpis.layout.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "layout")
@Data
public class Layout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 32)
    private String resolution;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    /**
     * 模块配置数组 JSON：[{id,name,type,x,y,w,h,zIndex},...]
     */
    @Column(nullable = false, columnDefinition = "JSON")
    private String modules;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
