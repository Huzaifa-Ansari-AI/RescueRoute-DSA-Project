package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, UUID> {
    Optional<Ambulance> findByFacilityId(UUID facilityId);
    List<Ambulance> findByAvailableNowTrue();
}
