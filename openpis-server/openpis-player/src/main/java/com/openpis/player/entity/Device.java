package com.openpis.player.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "device")
@Data
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 128)
    private String code;

    /**
     * online / offline
     */
    @Column(length = 32)
    private String status = "offline";

    @Column(name = "last_heartbeat")
    private LocalDateTime lastHeartbeat;
}
