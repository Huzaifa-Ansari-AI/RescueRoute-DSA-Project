package com.huzaifa.rescueroute.service.impl;

import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.entity.AdminLog;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.entity.User;
import com.huzaifa.rescueroute.entity.UserRole;
import com.huzaifa.rescueroute.entity.VerificationRequest;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.repository.specification.FacilitySpecifications;
import com.huzaifa.rescueroute.service.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of FacilityService.
 * Handles facility discovery, ID-based lookups, provider facility listings,
 * and safe soft-deletion/restoration lifecycle.
 */
@Service
@Transactional(readOnly = true)
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;
    private final FacilityMapper facilityMapper;
    private final UserRepository userRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final AdminLogRepository adminLogRepository;

    public FacilityServiceImpl(
            FacilityRepository facilityRepository,
            FacilityMapper facilityMapper) {
        this(facilityRepository, facilityMapper, null, null, null);
    }

    public FacilityServiceImpl(
            FacilityRepository facilityRepository,
            HospitalRepository hospitalRepository,
            BloodBankRepository bloodBankRepository,
            PharmacyRepository pharmacyRepository,
            AmbulanceRepository ambulanceRepository,
            ClinicRepository clinicRepository,
            FacilityMapper facilityMapper) {
        this(facilityRepository, facilityMapper, null, null, null);
    }

    @Autowired
    public FacilityServiceImpl(
            FacilityRepository facilityRepository,
            FacilityMapper facilityMapper,
            @Autowired(required = false) UserRepository userRepository,
            @Autowired(required = false) VerificationRequestRepository verificationRequestRepository,
            @Autowired(required = false) AdminLogRepository adminLogRepository) {
        this.facilityRepository = facilityRepository;
        this.facilityMapper = facilityMapper;
        this.userRepository = userRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.adminLogRepository = adminLogRepository;
    }

    public FacilityServiceImpl(
            FacilityRepository facilityRepository,
            HospitalRepository hospitalRepository,
            BloodBankRepository bloodBankRepository,
            PharmacyRepository pharmacyRepository,
            AmbulanceRepository ambulanceRepository,
            ClinicRepository clinicRepository,
            FacilityMapper facilityMapper,
            UserRepository userRepository,
            VerificationRequestRepository verificationRequestRepository,
            AdminLogRepository adminLogRepository) {
        this(facilityRepository, facilityMapper, userRepository, verificationRequestRepository, adminLogRepository);
    }

    @Override
    public List<FacilityResponseDto> getAllFacilities(String type, String city, Boolean active) {
        // Exclude soft-deleted facilities by default for normal discovery unless explicitly requested
        Boolean effectiveActive = active != null ? active : Boolean.TRUE;
        Specification<Facility> spec = FacilitySpecifications.withFilters(type, city, effectiveActive);
        List<Facility> facilities = facilityRepository.findAll(spec);
        return facilities.stream()
                .map(this::enrichAndMapFacility)
                .collect(Collectors.toList());
    }

    @Override
    public FacilityResponseDto getFacilityById(UUID id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));

        // Soft-deleted facilities are excluded from normal details access
        if (!facility.isActive()) {
            throw new ResourceNotFoundException("Facility not found with ID: " + id);
        }

        return enrichAndMapFacility(facility);
    }

    @Override
    @Transactional
    public FacilityResponseDto deactivateFacility(UUID id, String callerEmail) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));

        if (userRepository == null) {
            facility.setActive(false);
            facility.setDeletedAt(Instant.now());
            return enrichAndMapFacility(facilityRepository.save(facility));
        }

        User caller = userRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + callerEmail));

        boolean isAdmin = caller.getRole() == UserRole.ADMIN;
        boolean isOwner = facility.getCreatedBy() != null && facility.getCreatedBy().getId().equals(caller.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Unauthorized: You can only deactivate facilities that you own");
        }

        if (!facility.isActive()) {
            return enrichAndMapFacility(facility); // Already deactivated
        }

        facility.setActive(false);
        facility.setDeletedAt(Instant.now());
        facility.setHasPendingUpdate(false);
        facility.setUpdatedAt(Instant.now());

        // Cancel any pending verification requests for this facility
        if (verificationRequestRepository != null) {
            List<VerificationRequest> pending = verificationRequestRepository.findByFacilityIdAndStatus(id, "PENDING");
            for (VerificationRequest vr : pending) {
                vr.setStatus("REJECTED");
                vr.setReviewNotes("Auto-cancelled due to facility deactivation");
                vr.setReviewedAt(Instant.now());
                verificationRequestRepository.save(vr);
            }
        }

        Facility saved = facilityRepository.save(facility);

        // Record admin audit log
        if (adminLogRepository != null) {
            AdminLog log = AdminLog.builder()
                    .admin(isAdmin ? caller : null)
                    .actionType("FACILITY_DEACTIVATED")
                    .targetEntityType("FACILITY")
                    .targetEntityId(saved.getId())
                    .details(String.format("Facility '%s' (%s) deactivated by '%s'", saved.getName(), saved.getId(), caller.getEmail()))
                    .createdAt(Instant.now())
                    .build();
            adminLogRepository.save(log);
        }

        return enrichAndMapFacility(saved);
    }

    @Override
    @Transactional
    public FacilityResponseDto reactivateFacility(UUID id, String adminEmail) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));

        if (userRepository == null) {
            facility.setActive(true);
            facility.setDeletedAt(null);
            return enrichAndMapFacility(facilityRepository.save(facility));
        }

        User caller = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + adminEmail));

        if (caller.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only administrators can restore or reactivate facilities");
        }

        facility.setActive(true);
        facility.setDeletedAt(null);
        facility.setUpdatedAt(Instant.now());

        Facility saved = facilityRepository.save(facility);

        if (adminLogRepository != null) {
            AdminLog log = AdminLog.builder()
                    .admin(caller)
                    .actionType("FACILITY_REACTIVATED")
                    .targetEntityType("FACILITY")
                    .targetEntityId(saved.getId())
                    .details(String.format("Facility '%s' (%s) restored/reactivated by admin '%s'", saved.getName(), saved.getId(), caller.getEmail()))
                    .createdAt(Instant.now())
                    .build();
            adminLogRepository.save(log);
        }

        return enrichAndMapFacility(saved);
    }

    @Override
    public List<FacilityResponseDto> getFacilitiesByProvider(String providerEmail) {
        if (userRepository == null) {
            return List.of();
        }

        User provider = userRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + providerEmail));

        List<Facility> facilities;
        if (provider.getRole() == UserRole.ADMIN) {
            facilities = facilityRepository.findAll();
        } else {
            facilities = facilityRepository.findByCreatedById(provider.getId());
        }

        return facilities.stream()
                .map(this::enrichAndMapFacility)
                .collect(Collectors.toList());
    }

    @Override
    public List<FacilityResponseDto> getAdminFacilities(String type, String city, Boolean active, Boolean verified) {
        Specification<Facility> spec = FacilitySpecifications.withAdminFilters(type, city, active, verified);
        List<Facility> facilities = facilityRepository.findAll(spec);
        return facilities.stream()
                .map(this::enrichAndMapFacility)
                .collect(Collectors.toList());
    }

    /**
     * Converts the entity into FacilityResponseDto. Subtype associations are eagerly
     * joined via @EntityGraph on the repository layer or batch-fetched by Hibernate.
     */
    private FacilityResponseDto enrichAndMapFacility(Facility facility) {
        if (facility == null) {
            return null;
        }

        return facilityMapper.toDto(facility);
    }
}
