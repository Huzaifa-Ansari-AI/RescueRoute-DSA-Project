package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.BloodBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BloodBankRepository extends JpaRepository<BloodBank, UUID> {
    Optional<BloodBank> findByFacilityId(UUID facilityId);
}
