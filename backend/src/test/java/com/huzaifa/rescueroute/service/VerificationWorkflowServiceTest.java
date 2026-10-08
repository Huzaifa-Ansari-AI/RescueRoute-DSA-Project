package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.*;
import com.huzaifa.rescueroute.entity.*;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.service.impl.VerificationWorkflowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VerificationWorkflowServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private AdminLogRepository adminLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FacilityMapper facilityMapper;

    @InjectMocks
    private VerificationWorkflowServiceImpl verificationWorkflowService;

    private User providerUser;
    private User adminUser;
    private Facility testFacility;
    private VerificationRequest pendingRequest;

    @BeforeEach
    void setUp() {
        providerUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Dr. Provider")
                .email("provider@rescueroute.com")
                .role(UserRole.PROVIDER)
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("System Admin")
                .email("admin@rescueroute.com")
                .role(UserRole.ADMIN)
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        testFacility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Karachi Trauma Care")
                .facilityType("Hospital")
                .address("Stadium Road, Karachi")
                .city("Karachi")
                .latitude(24.8920)
                .longitude(67.0740)
                .phoneNumber("021-34930051")
                .isVerified(false)
                .isActive(true)
                .createdBy(providerUser)
                .createdAt(Instant.now())
                .build();

        pendingRequest = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .facility(testFacility)
                .submittedBy(providerUser)
                .status("PENDING")
                .submittedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Provider successfully creates facility, forced to unverified, verification request auto-created")
    void testCreateFacilityWithVerification_Success() {
        CreateFacilityRequestDto request = CreateFacilityRequestDto.builder()
                .name("Karachi Trauma Care")
                .facilityType("Hospital")
                .address("Stadium Road, Karachi")
                .city("Karachi")
                .latitude(24.8920)
                .longitude(67.0740)
                .phoneNumber("021-34930051")
                .hospitalDetails(HospitalDetailsDto.builder()
                        .emergencyAvailable(true)
                        .icuAvailable(true)
                        .notes("Top emergency hospital")
                        .build())
                .build();

        when(userRepository.findByEmail(providerUser.getEmail())).thenReturn(Optional.of(providerUser));
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(facilityMapper.toDto(any(Facility.class))).thenReturn(FacilityResponseDto.builder()
                .id(testFacility.getId())
                .name("Karachi Trauma Care")
                .isVerified(false)
                .build());

        FacilityResponseDto response = verificationWorkflowService.createFacilityWithVerification(
                request, providerUser.getEmail());

        assertNotNull(response);
        assertFalse(response.isVerified(), "Newly created facility must strictly be unverified");

        ArgumentCaptor<Facility> facilityCaptor = ArgumentCaptor.forClass(Facility.class);
        verify(facilityRepository).save(facilityCaptor.capture());
        Facility savedFacility = facilityCaptor.getValue();
        assertFalse(savedFacility.isVerified());
        assertEquals("Karachi Trauma Care", savedFacility.getName());
        assertEquals(providerUser, savedFacility.getCreatedBy());
        assertNotNull(savedFacility.getHospital());
        assertTrue(savedFacility.getHospital().isEmergencyAvailable());

        ArgumentCaptor<VerificationRequest> requestCaptor = ArgumentCaptor.forClass(VerificationRequest.class);
        verify(verificationRequestRepository).save(requestCaptor.capture());
        VerificationRequest savedRequest = requestCaptor.getValue();
        assertEquals("PENDING", savedRequest.getStatus());
        assertEquals(providerUser, savedRequest.getSubmittedBy());
    }

    @Test
    @DisplayName("Non-provider role is rejected with AccessDeniedException")
    void testCreateFacilityWithVerification_NonProvider_AccessDenied() {
        User regularUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .role(UserRole.USER)
                .build();

        when(userRepository.findByEmail(regularUser.getEmail())).thenReturn(Optional.of(regularUser));

        CreateFacilityRequestDto request = CreateFacilityRequestDto.builder()
                .name("Fake Clinic")
                .facilityType("Hospital")
                .address("Some street")
                .latitude(24.8)
                .longitude(67.0)
                .build();

        assertThrows(AccessDeniedException.class, () ->
                verificationWorkflowService.createFacilityWithVerification(request, regularUser.getEmail()));

        verify(facilityRepository, never()).save(any());
        verify(verificationRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid coordinates throw IllegalArgumentException")
    void testCreateFacilityWithVerification_InvalidCoordinates() {
        when(userRepository.findByEmail(providerUser.getEmail())).thenReturn(Optional.of(providerUser));

        CreateFacilityRequestDto request = CreateFacilityRequestDto.builder()
                .name("Invalid Coords")
                .facilityType("Pharmacy")
                .address("Some street")
                .latitude(150.0) // Invalid > 90
                .longitude(67.0)
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                verificationWorkflowService.createFacilityWithVerification(request, providerUser.getEmail()));
    }

    @Test
    @DisplayName("Admin approval sets facility verified, marks request APPROVED, and records admin audit log")
    void testProcessDecision_Approve_Success() {
        when(verificationRequestRepository.findById(pendingRequest.getId())).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationDecisionRequestDto decisionDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .reviewNotes("Verified by clinical inspection")
                .build();

        VerificationRequestResponseDto result = verificationWorkflowService.processDecision(
                pendingRequest.getId(), decisionDto, adminUser.getEmail());

        assertNotNull(result);
        assertEquals("APPROVED", result.getStatus());
        assertTrue(pendingRequest.getFacility().isVerified(), "Associated facility must now be verified");
        assertEquals("APPROVED", pendingRequest.getStatus());
        assertEquals(adminUser, pendingRequest.getReviewedBy());

        ArgumentCaptor<AdminLog> logCaptor = ArgumentCaptor.forClass(AdminLog.class);
        verify(adminLogRepository).save(logCaptor.capture());
        AdminLog savedLog = logCaptor.getValue();
        assertEquals("VERIFICATION_APPROVED", savedLog.getActionType());
        assertEquals(adminUser, savedLog.getAdmin());
        assertEquals("FACILITY", savedLog.getTargetEntityType());
        assertEquals(testFacility.getId(), savedLog.getTargetEntityId());
    }

    @Test
    @DisplayName("Admin rejection keeps facility unverified, marks request REJECTED, stores review notes, and writes audit log")
    void testProcessDecision_Reject_Success() {
        when(verificationRequestRepository.findById(pendingRequest.getId())).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(verificationRequestRepository.save(any(VerificationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationDecisionRequestDto decisionDto = VerificationDecisionRequestDto.builder()
                .decision("REJECTED")
                .reviewNotes("Missing licensing documentation")
                .build();

        VerificationRequestResponseDto result = verificationWorkflowService.processDecision(
                pendingRequest.getId(), decisionDto, adminUser.getEmail());

        assertNotNull(result);
        assertEquals("REJECTED", result.getStatus());
        assertFalse(pendingRequest.getFacility().isVerified(), "Facility must remain unverified upon rejection");
        assertEquals("REJECTED", pendingRequest.getStatus());
        assertEquals("Missing licensing documentation", pendingRequest.getReviewNotes());

        ArgumentCaptor<AdminLog> logCaptor = ArgumentCaptor.forClass(AdminLog.class);
        verify(adminLogRepository).save(logCaptor.capture());
        AdminLog savedLog = logCaptor.getValue();
        assertEquals("VERIFICATION_REJECTED", savedLog.getActionType());
        assertTrue(savedLog.getDetails().contains("Missing licensing documentation"));
    }

    @Test
    @DisplayName("Rejection without review notes is rejected with IllegalArgumentException")
    void testProcessDecision_RejectWithoutNotes_ThrowsException() {
        when(verificationRequestRepository.findById(pendingRequest.getId())).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        VerificationDecisionRequestDto decisionDto = VerificationDecisionRequestDto.builder()
                .decision("REJECTED")
                .reviewNotes("   ") // blank notes
                .build();

        assertThrows(IllegalArgumentException.class, () ->
                verificationWorkflowService.processDecision(pendingRequest.getId(), decisionDto, adminUser.getEmail()));

        verify(adminLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("Attempting to re-process an already processed request throws IllegalStateException")
    void testProcessDecision_AlreadyProcessed_ThrowsException() {
        pendingRequest.setStatus("APPROVED");
        when(verificationRequestRepository.findById(pendingRequest.getId())).thenReturn(Optional.of(pendingRequest));

        VerificationDecisionRequestDto decisionDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .build();

        assertThrows(IllegalStateException.class, () ->
                verificationWorkflowService.processDecision(pendingRequest.getId(), decisionDto, adminUser.getEmail()));
    }

    @Test
    @DisplayName("Provider cannot approve their own verification request")
    void testProcessDecision_SelfApproval_ConflictThrowsAccessDenied() {
        // Submitter is providerUser, and providerUser is also passed as admin reviewer
        pendingRequest.setSubmittedBy(adminUser); // Admin submitted it
        when(verificationRequestRepository.findById(pendingRequest.getId())).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByEmail(adminUser.getEmail())).thenReturn(Optional.of(adminUser));

        VerificationDecisionRequestDto decisionDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                verificationWorkflowService.processDecision(pendingRequest.getId(), decisionDto, adminUser.getEmail()));
    }

    @Test
    @DisplayName("Non-existent verification request throws ResourceNotFoundException")
    void testGetVerificationRequestById_NotFound() {
        UUID randomId = UUID.randomUUID();
        when(verificationRequestRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                verificationWorkflowService.getVerificationRequestById(randomId));
    }
}
