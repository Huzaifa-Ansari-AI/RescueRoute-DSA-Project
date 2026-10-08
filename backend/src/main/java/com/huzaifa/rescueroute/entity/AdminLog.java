package com.huzaifa.rescueroute.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log recording administrative moderations and status updates.
 */
@Entity
@Table(name = "admin_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;

    @Column(name = "target_entity_type", nullable = false, length = 100)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
