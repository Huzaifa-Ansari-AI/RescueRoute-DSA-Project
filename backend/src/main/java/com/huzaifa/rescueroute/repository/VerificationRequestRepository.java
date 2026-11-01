package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.VerificationRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

    @EntityGraph(attributePaths = {"facility", "submittedBy", "reviewedBy", "facility.hospital", "facility.bloodBank", "facility.pharmacy", "facility.ambulance", "facility.clinic"})
    List<VerificationRequest> findByStatus(String status);

    @Override
    @EntityGraph(attributePaths = {"facility", "submittedBy", "reviewedBy", "facility.hospital", "facility.bloodBank", "facility.pharmacy", "facility.ambulance", "facility.clinic"})
    List<VerificationRequest> findAll();

    @Override
    @EntityGraph(attributePaths = {"facility", "submittedBy", "reviewedBy", "facility.hospital", "facility.bloodBank", "facility.pharmacy", "facility.ambulance", "facility.clinic"})
    Optional<VerificationRequest> findById(UUID id);

    List<VerificationRequest> findByFacilityId(UUID facilityId);
    List<VerificationRequest> findByFacilityIdAndStatus(UUID facilityId, String status);
}
