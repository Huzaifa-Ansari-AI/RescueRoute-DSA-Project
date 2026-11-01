package com.huzaifa.rescueroute.service.impl;

import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.*;
import com.huzaifa.rescueroute.entity.*;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.service.VerificationWorkflowService;
import com.huzaifa.rescueroute.util.HaversineUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of the Provider facility submission and Admin verification workflow.
 * Guaranteed transactional boundaries across base facility, subtypes, verification requests, and audit logs.
 */
@Service
public class VerificationWorkflowServiceImpl implements VerificationWorkflowService {

    private final FacilityRepository facilityRepository;
    private final VerificationRequestRepository verificationRequestRepository;
    private final AdminLogRepository adminLogRepository;
    private final UserRepository userRepository;
    private final FacilityMapper facilityMapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public VerificationWorkflowServiceImpl(
            FacilityRepository facilityRepository,
            VerificationRequestRepository verificationRequestRepository,
            AdminLogRepository adminLogRepository,
            UserRepository userRepository,
            FacilityMapper facilityMapper) {
        this(facilityRepository, verificationRequestRepository, adminLogRepository,
             userRepository, facilityMapper, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public VerificationWorkflowServiceImpl(
            FacilityRepository facilityRepository,
            VerificationRequestRepository verificationRequestRepository,
            AdminLogRepository adminLogRepository,
            UserRepository userRepository,
            FacilityMapper facilityMapper,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.facilityRepository = facilityRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.adminLogRepository = adminLogRepository;
        this.userRepository = userRepository;
        this.facilityMapper = facilityMapper;
        this.objectMapper = objectMapper != null ? objectMapper : new com.fasterxml.jackson.databind.ObjectMapper();
    }

    @Override
    @Transactional
    public FacilityResponseDto createFacilityWithVerification(CreateFacilityRequestDto request, String providerEmail) {
        User provider = userRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + providerEmail));

        if (provider.getRole() != UserRole.PROVIDER && provider.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only authorized medical providers can submit facilities for verification");
        }

        // Validate coordinates
        HaversineUtil.validateCoordinate(request.getLatitude(), request.getLongitude(), "Facility");

        // Construct base facility entity
        Facility facility = Facility.builder()
                .name(request.getName().trim())
                .facilityType(request.getFacilityType())
                .address(request.getAddress().trim())
                .city(request.getCity() != null ? request.getCity().trim() : "Karachi")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .phoneNumber(request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null)
                .isVerified(false) // Strictly enforce unverified initial state
                .isActive(true)
                .createdBy(provider)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        // Attach appropriate subtype details
        attachSubtype(facility, request);

        // Save facility and its cascade-persisted subtype
        Facility savedFacility = facilityRepository.save(facility);

        // Automatically create corresponding PENDING verification request
        VerificationRequest verificationRequest = VerificationRequest.builder()
                .facility(savedFacility)
                .submittedBy(provider)
                .status("PENDING")
                .submittedAt(Instant.now())
                .build();

        verificationRequestRepository.save(verificationRequest);

        return facilityMapper.toDto(savedFacility);
    }

    @Override
    @Transactional
    public FacilityResponseDto submitFacilityUpdate(UUID facilityId, UpdateFacilityRequestDto request, String providerEmail) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + facilityId));

        if (!facility.isActive()) {
            throw new IllegalStateException("Cannot update a deactivated facility. Please contact administrator to restore it.");
        }

        User caller = userRepository.findByEmail(providerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + providerEmail));

        boolean isAdmin = caller.getRole() == UserRole.ADMIN;
        boolean isOwner = facility.getCreatedBy() != null && facility.getCreatedBy().getId().equals(caller.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("Unauthorized: You can only edit facilities that you own");
        }

        // Check if there is already a PENDING verification request
        List<VerificationRequest> pending = verificationRequestRepository.findByFacilityIdAndStatus(facilityId, "PENDING");
        if (!pending.isEmpty() || facility.isHasPendingUpdate()) {
            throw new IllegalStateException("A pending verification request is already under review for this facility");
        }

        // Validate coordinates
        HaversineUtil.validateCoordinate(request.getLatitude(), request.getLongitude(), "Facility");

        // Serialize proposed changes to JSON
        String proposedChangesJson;
        try {
            proposedChangesJson = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize proposed facility changes", e);
        }

        // Create PENDING verification request of type UPDATE
        VerificationRequest vr = VerificationRequest.builder()
                .facility(facility)
                .submittedBy(caller)
                .status("PENDING")
                .requestType("UPDATE")
                .proposedChanges(proposedChangesJson)
                .submittedAt(Instant.now())
                .build();
        verificationRequestRepository.save(vr);

        // Mark facility hasPendingUpdate = true while leaving live data untouched
        facility.setHasPendingUpdate(true);
        facility.setUpdatedAt(Instant.now());
        Facility saved = facilityRepository.save(facility);

        return facilityMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationRequestResponseDto> getVerificationRequests(String status) {
        List<VerificationRequest> requests;
        if (status != null && !status.trim().isEmpty()) {
            requests = verificationRequestRepository.findByStatus(status.trim().toUpperCase());
        } else {
            requests = verificationRequestRepository.findAll();
        }

        return requests.stream()
                .map(this::mapToVerificationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationRequestResponseDto getVerificationRequestById(UUID requestId) {
        VerificationRequest request = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found with ID: " + requestId));
        return mapToVerificationDto(request);
    }

    @Override
    @Transactional
    public VerificationRequestResponseDto processDecision(
            UUID requestId, VerificationDecisionRequestDto decisionDto, String adminEmail) {

        VerificationRequest verificationRequest = verificationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found with ID: " + requestId));

        if (!"PENDING".equalsIgnoreCase(verificationRequest.getStatus())) {
            throw new IllegalStateException("Verification request has already been processed with status: "
                    + verificationRequest.getStatus());
        }

        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

        if (adminUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only administrators can approve or reject verification requests");
        }

        // Prevent provider self-approval conflict
        if (verificationRequest.getSubmittedBy() != null &&
                verificationRequest.getSubmittedBy().getId().equals(adminUser.getId())) {
            throw new AccessDeniedException("Conflict of interest: A user cannot approve or reject their own submitted facility");
        }

        String resolvedDecision = decisionDto.getResolvedDecision();
        if (resolvedDecision == null || (!"APPROVED".equals(resolvedDecision) && !"REJECTED".equals(resolvedDecision))) {
            throw new IllegalArgumentException("Decision must be either 'APPROVED' or 'REJECTED'");
        }

        if ("REJECTED".equals(resolvedDecision) &&
                (decisionDto.getReviewNotes() == null || decisionDto.getReviewNotes().trim().isEmpty())) {
            throw new IllegalArgumentException("Review notes specifying a reason are required when rejecting a verification request");
        }

        // 1. Update associated facility
        Facility facility = verificationRequest.getFacility();
        String requestType = verificationRequest.getRequestType() != null ? verificationRequest.getRequestType() : "CREATION";
        String actionType;

        if ("UPDATE".equalsIgnoreCase(requestType)) {
            if ("APPROVED".equals(resolvedDecision)) {
                applyProposedChanges(facility, verificationRequest.getProposedChanges());
                facility.setVerified(true);
                facility.setHasPendingUpdate(false);
                actionType = "FACILITY_UPDATE_APPROVED";
            } else {
                facility.setHasPendingUpdate(false);
                actionType = "FACILITY_UPDATE_REJECTED";
            }
        } else {
            if ("APPROVED".equals(resolvedDecision)) {
                facility.setVerified(true);
                actionType = "VERIFICATION_APPROVED";
            } else {
                facility.setVerified(false);
                actionType = "VERIFICATION_REJECTED";
            }
            facility.setHasPendingUpdate(false);
        }

        facility.setUpdatedAt(Instant.now());
        facilityRepository.save(facility);

        // 2. Update verification request
        verificationRequest.setStatus(resolvedDecision);
        verificationRequest.setReviewedBy(adminUser);
        verificationRequest.setReviewedAt(Instant.now());
        verificationRequest.setReviewNotes(decisionDto.getReviewNotes() != null ? decisionDto.getReviewNotes().trim() : null);
        VerificationRequest updatedRequest = verificationRequestRepository.save(verificationRequest);

        // 3. Record immutable admin audit log within same transaction
        AdminLog log = AdminLog.builder()
                .admin(adminUser)
                .actionType(actionType)
                .targetEntityType("FACILITY")
                .targetEntityId(facility.getId())
                .details(String.format("Admin '%s' processed %s request as %s for facility '%s' (%s). Notes: %s",
                        adminUser.getEmail(),
                        requestType,
                        resolvedDecision.toLowerCase(),
                        facility.getName(),
                        facility.getId(),
                        decisionDto.getReviewNotes() != null ? decisionDto.getReviewNotes().trim() : "None"))
                .createdAt(Instant.now())
                .build();
        adminLogRepository.save(log);

        return mapToVerificationDto(updatedRequest);
    }

    private void attachSubtype(Facility facility, CreateFacilityRequestDto request) {
        String type = request.getFacilityType();

        if ("Hospital".equalsIgnoreCase(type)) {
            HospitalDetailsDto h = request.getHospitalDetails();
            Hospital hospital = Hospital.builder()
                    .facility(facility)
                    .emergencyAvailable(h != null && h.isEmergencyAvailable())
                    .icuAvailable(h != null && h.isIcuAvailable())
                    .traumaCenterAvailable(h != null && h.isTraumaCenterAvailable())
                    .cardiologyAvailable(h != null && h.isCardiologyAvailable())
                    .pediatricsAvailable(h != null && h.isPediatricsAvailable())
                    .maternityAvailable(h != null && h.isMaternityAvailable())
                    .notes(h != null ? h.getNotes() : null)
                    .build();
            facility.setHospital(hospital);
        } else if ("Blood Bank".equalsIgnoreCase(type)) {
            BloodBankDetailsDto b = request.getBloodBankDetails();
            BloodBank bloodBank = BloodBank.builder()
                    .facility(facility)
                    .oPositive(b != null && b.isOPositive())
                    .oNegative(b != null && b.isONegative())
                    .aPositive(b != null && b.isAPositive())
                    .aNegative(b != null && b.isANegative())
                    .bPositive(b != null && b.isBPositive())
                    .bNegative(b != null && b.isBNegative())
                    .abPositive(b != null && b.isAbPositive())
                    .abNegative(b != null && b.isAbNegative())
                    .stockUpdatedAt(b != null ? b.getStockUpdatedAt() : null)
                    .notes(b != null ? b.getNotes() : null)
                    .build();
            facility.setBloodBank(bloodBank);
        } else if ("Pharmacy".equalsIgnoreCase(type)) {
            PharmacyDetailsDto p = request.getPharmacyDetails();
            Pharmacy pharmacy = Pharmacy.builder()
                    .facility(facility)
                    .emergencyMedicinesAvailable(p != null && p.isEmergencyMedicinesAvailable())
                    .open24Hours(p != null && p.isOpen24Hours())
                    .prescriptionRequired(p == null || p.isPrescriptionRequired())
                    .deliveryAvailable(p != null && p.isDeliveryAvailable())
                    .notes(p != null ? p.getNotes() : null)
                    .build();
            facility.setPharmacy(pharmacy);
        } else if ("Ambulance".equalsIgnoreCase(type)) {
            AmbulanceDetailsDto a = request.getAmbulanceDetails();
            Ambulance ambulance = Ambulance.builder()
                    .facility(facility)
                    .availableNow(a != null && a.isAvailableNow())
                    .responseTimeMinutes(a != null ? a.getResponseTimeMinutes() : 15)
                    .coverageArea(a != null ? a.getCoverageArea() : "Karachi Central")
                    .equippedForIcuTransfer(a != null && a.isEquippedForIcuTransfer())
                    .notes(a != null ? a.getNotes() : null)
                    .build();
            facility.setAmbulance(ambulance);
        } else if ("Clinic".equalsIgnoreCase(type)) {
            ClinicDetailsDto c = request.getClinicDetails();
            Clinic clinic = Clinic.builder()
                    .facility(facility)
                    .consultationAvailable(c == null || c.isConsultationAvailable())
                    .specialties(c != null ? c.getSpecialties() : null)
                    .openWeekends(c != null && c.isOpenWeekends())
                    .notes(c != null ? c.getNotes() : null)
                    .build();
            facility.setClinic(clinic);
        } else {
            throw new IllegalArgumentException("Unsupported facility type: " + type);
        }
    }

    private void applyProposedChanges(Facility facility, String proposedChangesJson) {
        if (proposedChangesJson == null || proposedChangesJson.trim().isEmpty()) {
            return;
        }

        UpdateFacilityRequestDto changes;
        try {
            changes = objectMapper.readValue(proposedChangesJson, UpdateFacilityRequestDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize proposed facility changes", e);
        }

        facility.setName(changes.getName().trim());
        facility.setAddress(changes.getAddress().trim());
        facility.setCity(changes.getCity() != null ? changes.getCity().trim() : "Karachi");
        facility.setLatitude(changes.getLatitude());
        facility.setLongitude(changes.getLongitude());
        facility.setPhoneNumber(changes.getPhoneNumber() != null ? changes.getPhoneNumber().trim() : null);

        String type = facility.getFacilityType();
        if ("Hospital".equalsIgnoreCase(type) && changes.getHospitalDetails() != null) {
            Hospital h = facility.getHospital();
            HospitalDetailsDto dto = changes.getHospitalDetails();
            if (h == null) {
                h = Hospital.builder().facility(facility).build();
                facility.setHospital(h);
            }
            h.setEmergencyAvailable(dto.isEmergencyAvailable());
            h.setIcuAvailable(dto.isIcuAvailable());
            h.setTraumaCenterAvailable(dto.isTraumaCenterAvailable());
            h.setCardiologyAvailable(dto.isCardiologyAvailable());
            h.setPediatricsAvailable(dto.isPediatricsAvailable());
            h.setMaternityAvailable(dto.isMaternityAvailable());
            h.setNotes(dto.getNotes());
        } else if ("Blood Bank".equalsIgnoreCase(type) && changes.getBloodBankDetails() != null) {
            BloodBank b = facility.getBloodBank();
            BloodBankDetailsDto dto = changes.getBloodBankDetails();
            if (b == null) {
                b = BloodBank.builder().facility(facility).build();
                facility.setBloodBank(b);
            }
            b.setOPositive(dto.isOPositive());
            b.setONegative(dto.isONegative());
            b.setAPositive(dto.isAPositive());
            b.setANegative(dto.isANegative());
            b.setBPositive(dto.isBPositive());
            b.setBNegative(dto.isBNegative());
            b.setAbPositive(dto.isAbPositive());
            b.setAbNegative(dto.isAbNegative());
            b.setStockUpdatedAt(dto.getStockUpdatedAt());
            b.setNotes(dto.getNotes());
        } else if ("Pharmacy".equalsIgnoreCase(type) && changes.getPharmacyDetails() != null) {
            Pharmacy p = facility.getPharmacy();
            PharmacyDetailsDto dto = changes.getPharmacyDetails();
            if (p == null) {
                p = Pharmacy.builder().facility(facility).build();
                facility.setPharmacy(p);
            }
            p.setOpen24Hours(dto.isOpen24Hours());
            p.setEmergencyMedicinesAvailable(dto.isEmergencyMedicinesAvailable());
            p.setPrescriptionRequired(dto.isPrescriptionRequired());
            p.setDeliveryAvailable(dto.isDeliveryAvailable());
            p.setNotes(dto.getNotes());
        } else if ("Ambulance".equalsIgnoreCase(type) && changes.getAmbulanceDetails() != null) {
            Ambulance a = facility.getAmbulance();
            AmbulanceDetailsDto dto = changes.getAmbulanceDetails();
            if (a == null) {
                a = Ambulance.builder().facility(facility).build();
                facility.setAmbulance(a);
            }
            a.setAvailableNow(dto.isAvailableNow());
            a.setResponseTimeMinutes(dto.getResponseTimeMinutes() != null ? dto.getResponseTimeMinutes() : 15);
            a.setCoverageArea(dto.getCoverageArea() != null ? dto.getCoverageArea() : "Karachi Central");
            a.setEquippedForIcuTransfer(dto.isEquippedForIcuTransfer());
            a.setNotes(dto.getNotes());
        } else if ("Clinic".equalsIgnoreCase(type) && changes.getClinicDetails() != null) {
            Clinic c = facility.getClinic();
            ClinicDetailsDto dto = changes.getClinicDetails();
            if (c == null) {
                c = Clinic.builder().facility(facility).build();
                facility.setClinic(c);
            }
            c.setConsultationAvailable(dto.isConsultationAvailable());
            c.setSpecialties(dto.getSpecialties());
            c.setOpenWeekends(dto.isOpenWeekends());
            c.setNotes(dto.getNotes());
        }
    }

    private VerificationRequestResponseDto mapToVerificationDto(VerificationRequest request) {
        if (request == null) {
            return null;
        }

        Facility facility = request.getFacility();
        User submitter = request.getSubmittedBy();
        User reviewer = request.getReviewedBy();

        UserResponseDto submitterDto = submitter != null ? UserResponseDto.builder()
                .id(submitter.getId())
                .fullName(submitter.getFullName())
                .email(submitter.getEmail())
                .role(submitter.getRole())
                .isActive(submitter.isActive())
                .createdAt(submitter.getCreatedAt())
                .build() : null;

        UserResponseDto reviewerDto = reviewer != null ? UserResponseDto.builder()
                .id(reviewer.getId())
                .fullName(reviewer.getFullName())
                .email(reviewer.getEmail())
                .role(reviewer.getRole())
                .isActive(reviewer.isActive())
                .createdAt(reviewer.getCreatedAt())
                .build() : null;

        UpdateFacilityRequestDto parsedChanges = null;
        if (request.getProposedChanges() != null && !request.getProposedChanges().trim().isEmpty()) {
            try {
                parsedChanges = objectMapper.readValue(request.getProposedChanges(), UpdateFacilityRequestDto.class);
            } catch (Exception ignored) {
            }
        }

        return VerificationRequestResponseDto.builder()
                .id(request.getId())
                .facilityId(facility != null ? facility.getId() : null)
                .facilityName(facility != null ? facility.getName() : null)
                .facilityType(facility != null ? facility.getFacilityType() : null)
                .facilityAddress(facility != null ? facility.getAddress() : null)
                .facilityCity(facility != null ? facility.getCity() : null)
                .facilityLatitude(facility != null ? facility.getLatitude() : null)
                .facilityLongitude(facility != null ? facility.getLongitude() : null)
                .facilityPhoneNumber(facility != null ? facility.getPhoneNumber() : null)
                .facilityVerified(facility != null && facility.isVerified())
                .facilityActive(facility != null ? facility.isActive() : null)
                .facilityDeletedAt(facility != null ? facility.getDeletedAt() : null)
                .facilityHasPendingUpdate(facility != null ? facility.isHasPendingUpdate() : null)
                .submittedBy(submitterDto)
                .status(request.getStatus())
                .requestType(request.getRequestType() != null ? request.getRequestType() : "CREATION")
                .proposedChanges(parsedChanges)
                .reviewedBy(reviewerDto)
                .reviewNotes(request.getReviewNotes())
                .submittedAt(request.getSubmittedAt())
                .reviewedAt(request.getReviewedAt())
                .facilityDetails(facility != null ? facilityMapper.toDto(facility) : null)
                .build();
    }
}
