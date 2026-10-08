package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {
    Optional<Hospital> findByFacilityId(UUID facilityId);
    List<Hospital> findByEmergencyAvailableTrue();
    List<Hospital> findByIcuAvailableTrue();
}
