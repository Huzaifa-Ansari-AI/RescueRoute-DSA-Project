package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
    Optional<Clinic> findByFacilityId(UUID facilityId);
    List<Clinic> findByConsultationAvailableTrue();
    List<Clinic> findByOpenWeekendsTrue();
}
