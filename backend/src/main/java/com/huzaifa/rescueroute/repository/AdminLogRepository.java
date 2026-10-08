package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.AdminLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for AdminLog persistence and audit tracking.
 */
@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, UUID> {
    List<AdminLog> findByAdminId(UUID adminId);
    List<AdminLog> findByTargetEntityId(UUID targetEntityId);
    List<AdminLog> findByTargetEntityType(String targetEntityType);
}
