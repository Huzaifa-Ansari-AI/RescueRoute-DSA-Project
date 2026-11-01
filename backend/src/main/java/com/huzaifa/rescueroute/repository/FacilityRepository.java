package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.Facility;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, UUID>, JpaSpecificationExecutor<Facility> {

    @Override
    @EntityGraph(attributePaths = {"hospital", "bloodBank", "pharmacy", "ambulance", "clinic"})
    List<Facility> findAll(Specification<Facility> spec);

    @Override
    @EntityGraph(attributePaths = {"hospital", "bloodBank", "pharmacy", "ambulance", "clinic"})
    List<Facility> findAll();

    @Override
    @EntityGraph(attributePaths = {"hospital", "bloodBank", "pharmacy", "ambulance", "clinic"})
    java.util.Optional<Facility> findById(UUID id);

    List<Facility> findByFacilityType(String facilityType);
    List<Facility> findByCity(String city);
    List<Facility> findByIsActiveTrue();
    List<Facility> findByFacilityTypeAndIsActiveTrue(String facilityType);
    List<Facility> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    List<Facility> findByIsVerifiedTrueAndIsActiveTrue();

    @EntityGraph(attributePaths = {"hospital", "bloodBank", "pharmacy", "ambulance", "clinic"})
    List<Facility> findByCreatedById(UUID userId);

    List<Facility> findByCreatedByIdAndIsActiveTrue(UUID userId);
}
