package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.VerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {
    List<VerificationRequest> findByStatus(String status);
    List<VerificationRequest> findByFacilityId(UUID facilityId);
}
