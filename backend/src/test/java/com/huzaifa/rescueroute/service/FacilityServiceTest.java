package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.entity.Hospital;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.service.impl.FacilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import com.huzaifa.rescueroute.entity.AdminLog;
import com.huzaifa.rescueroute.entity.User;
import com.huzaifa.rescueroute.entity.UserRole;
import com.huzaifa.rescueroute.entity.VerificationRequest;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.mockito.ArgumentMatchers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private BloodBankRepository bloodBankRepository;

    @Mock
    private PharmacyRepository pharmacyRepository;

    @Mock
    private AmbulanceRepository ambulanceRepository;

    @Mock
    private ClinicRepository clinicRepository;

    @Mock
    private VerificationRequestRepository verificationRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminLogRepository adminLogRepository;

    private FacilityMapper facilityMapper;
    private FacilityService facilityService;

    @BeforeEach
    void setUp() {
        facilityMapper = new FacilityMapper();
        facilityService = new FacilityServiceImpl(
                facilityRepository,
                hospitalRepository,
                bloodBankRepository,
                pharmacyRepository,
                ambulanceRepository,
                clinicRepository,
                facilityMapper,
                userRepository,
                verificationRequestRepository,
                adminLogRepository
        );
    }

    @Test
    @DisplayName("Retrieve all facilities without filters")
    void testGetAllFacilities_NoFilters() {
        UUID id = UUID.randomUUID();
        Facility facility = Facility.builder()
                .id(id)
                .name("Aga Khan University Hospital")
                .facilityType("Hospital")
                .address("Stadium Road, Karachi")
                .city("Karachi")
                .latitude(24.8924)
                .longitude(67.0747)
                .phoneNumber("+92-21-111-911-911")
                .isVerified(true)
                .isActive(true)
                .build();

        when(facilityRepository.findAll(ArgumentMatchers.<Specification<Facility>>any())).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Aga Khan University Hospital", result.get(0).getName());
        assertEquals("Hospital", result.get(0).getFacilityType());
        assertTrue(result.get(0).isVerified());
        assertTrue(result.get(0).isActive());
        verify(facilityRepository, times(1)).findAll(ArgumentMatchers.<Specification<Facility>>any());
    }

    @Test
    @DisplayName("Filter facilities by facility type")
    void testGetAllFacilities_FilterByType() {
        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Indus Hospital")
                .facilityType("Hospital")
                .city("Karachi")
                .address("Korangi, Karachi")
                .latitude(24.8415)
                .longitude(67.1264)
                .isVerified(true)
                .isActive(true)
                .build();

        when(facilityRepository.findAll(ArgumentMatchers.<Specification<Facility>>any())).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities("Hospital", null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Indus Hospital", result.get(0).getName());
        verify(facilityRepository, times(1)).findAll(ArgumentMatchers.<Specification<Facility>>any());
    }

    @Test
    @DisplayName("Filter facilities by city")
    void testGetAllFacilities_FilterByCity() {
        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Karachi Rescue Center")
                .facilityType("Hospital")
                .city("Karachi")
                .address("Saddar, Karachi")
                .latitude(24.8600)
                .longitude(67.0100)
                .isActive(true)
                .build();

        when(facilityRepository.findAll(ArgumentMatchers.<Specification<Facility>>any())).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities(null, "Karachi", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Karachi", result.get(0).getCity());
        verify(facilityRepository, times(1)).findAll(ArgumentMatchers.<Specification<Facility>>any());
    }

    @Test
    @DisplayName("Filter facilities by active status")
    void testGetAllFacilities_FilterByActive() {
        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Active Facility")
                .facilityType("Pharmacy")
                .city("Karachi")
                .address("Gulshan, Karachi")
                .latitude(24.9100)
                .longitude(67.0900)
                .isActive(true)
                .build();

        when(facilityRepository.findAll(ArgumentMatchers.<Specification<Facility>>any())).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities(null, null, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
        verify(facilityRepository, times(1)).findAll(ArgumentMatchers.<Specification<Facility>>any());
    }

    @Test
    @DisplayName("Filter facilities by combined criteria (type, city, active)")
    void testGetAllFacilities_CombinedFilters() {
        when(facilityRepository.findAll(ArgumentMatchers.<Specification<Facility>>any())).thenReturn(Collections.emptyList());

        List<FacilityResponseDto> result = facilityService.getAllFacilities("Ambulance", "Karachi", true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(facilityRepository, times(1)).findAll(ArgumentMatchers.<Specification<Facility>>any());
    }

    @Test
    @DisplayName("Retrieve facility by ID with subtype details when found")
    void testGetFacilityById_Found() {
        UUID facilityId = UUID.randomUUID();
        Facility facility = Facility.builder()
                .id(facilityId)
                .name("Civil Hospital Karachi")
                .facilityType("Hospital")
                .address("Mission Road, Karachi")
                .city("Karachi")
                .latitude(24.8596)
                .longitude(67.0108)
                .phoneNumber("+92-21-99215740")
                .isVerified(true)
                .isActive(true)
                .build();

        Hospital hospital = Hospital.builder()
                .id(UUID.randomUUID())
                .facility(facility)
                .emergencyAvailable(true)
                .icuAvailable(true)
                .traumaCenterAvailable(true)
                .cardiologyAvailable(true)
                .pediatricsAvailable(true)
                .maternityAvailable(true)
                .notes("Large public tertiary hospital with trauma centre")
                .build();

        facility.setHospital(hospital);

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(facility));

        FacilityResponseDto result = facilityService.getFacilityById(facilityId);

        assertNotNull(result);
        assertEquals(facilityId, result.getId());
        assertEquals("Civil Hospital Karachi", result.getName());
        assertEquals("Hospital", result.getFacilityType());
        assertNotNull(result.getHospitalDetails());
        assertTrue(result.getHospitalDetails().isEmergencyAvailable());
        assertTrue(result.getHospitalDetails().isTraumaCenterAvailable());
        assertEquals("Large public tertiary hospital with trauma centre", result.getHospitalDetails().getNotes());
        verify(facilityRepository, times(1)).findById(facilityId);
    }

    @Test
    @DisplayName("Retrieve facility by ID throws ResourceNotFoundException when not found")
    void testGetFacilityById_NotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(facilityRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> facilityService.getFacilityById(nonExistentId)
        );

        assertTrue(exception.getMessage().contains("Facility not found with ID: " + nonExistentId));
        verify(facilityRepository, times(1)).findById(nonExistentId);
    }

    @Test
    @DisplayName("Retrieve facility by ID throws ResourceNotFoundException if facility is deactivated (soft-deleted)")
    void testGetFacilityById_Deactivated_ThrowsResourceNotFound() {
        UUID facilityId = UUID.randomUUID();
        Facility deactivated = Facility.builder()
                .id(facilityId)
                .name("Old Deactivated Clinic")
                .facilityType("Clinic")
                .isActive(false)
                .build();

        when(facilityRepository.findById(facilityId)).thenReturn(Optional.of(deactivated));

        assertThrows(ResourceNotFoundException.class, () ->
                facilityService.getFacilityById(facilityId));
    }

    @Test
    @DisplayName("Provider deactivates owned facility: sets isActive false, sets deletedAt, cancels pending verification, logs audit")
    void testDeactivateFacility_Success() {
        User provider = User.builder()
                .id(UUID.randomUUID())
                .email("dr.provider@rescueroute.com")
                .role(UserRole.PROVIDER)
                .build();

        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Provider Clinic")
                .facilityType("Clinic")
                .isActive(true)
                .createdBy(provider)
                .build();

        VerificationRequest pendingReq = VerificationRequest.builder()
                .id(UUID.randomUUID())
                .facility(facility)
                .status("PENDING")
                .build();

        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        when(verificationRequestRepository.findByFacilityIdAndStatus(facility.getId(), "PENDING"))
                .thenReturn(List.of(pendingReq));
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facilityService.deactivateFacility(facility.getId(), provider.getEmail());

        assertFalse(facility.isActive(), "Facility must be marked isActive = false");
        assertNotNull(facility.getDeletedAt(), "deletedAt timestamp must be recorded");
        assertEquals("REJECTED", pendingReq.getStatus(), "Pending verification requests must be marked REJECTED upon deactivation");
        assertTrue(pendingReq.getReviewNotes().contains("deactivation"));

        ArgumentCaptor<AdminLog> logCaptor = ArgumentCaptor.forClass(AdminLog.class);
        verify(adminLogRepository).save(logCaptor.capture());
        assertEquals("FACILITY_DEACTIVATED", logCaptor.getValue().getActionType());
    }

    @Test
    @DisplayName("Unauthorized provider cannot deactivate another provider's facility (AccessDeniedException)")
    void testDeactivateFacility_CrossProvider_AccessDenied() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .email("owner@rescueroute.com")
                .role(UserRole.PROVIDER)
                .build();

        User attacker = User.builder()
                .id(UUID.randomUUID())
                .email("attacker@rescueroute.com")
                .role(UserRole.PROVIDER)
                .build();

        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Protected Clinic")
                .facilityType("Clinic")
                .isActive(true)
                .createdBy(owner)
                .build();

        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(userRepository.findByEmail(attacker.getEmail())).thenReturn(Optional.of(attacker));

        assertThrows(AccessDeniedException.class, () ->
                facilityService.deactivateFacility(facility.getId(), attacker.getEmail()));

        assertTrue(facility.isActive());
        verify(facilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin reactivates deactivated facility: sets isActive true, clears deletedAt, logs audit")
    void testReactivateFacility_AdminSuccess() {
        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@rescueroute.com")
                .role(UserRole.ADMIN)
                .build();

        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Deactivated Clinic")
                .facilityType("Clinic")
                .isActive(false)
                .deletedAt(Instant.now())
                .build();

        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facilityService.reactivateFacility(facility.getId(), admin.getEmail());

        assertTrue(facility.isActive(), "Facility must be reactivated");
        assertNull(facility.getDeletedAt(), "deletedAt must be cleared upon reactivation");

        ArgumentCaptor<AdminLog> logCaptor = ArgumentCaptor.forClass(AdminLog.class);
        verify(adminLogRepository).save(logCaptor.capture());
        assertEquals("FACILITY_REACTIVATED", logCaptor.getValue().getActionType());
    }

    @Test
    @DisplayName("Non-admin user cannot reactivate facility (AccessDeniedException)")
    void testReactivateFacility_NonAdmin_AccessDenied() {
        User provider = User.builder()
                .id(UUID.randomUUID())
                .email("provider@rescueroute.com")
                .role(UserRole.PROVIDER)
                .build();

        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Deactivated Clinic")
                .facilityType("Clinic")
                .isActive(false)
                .build();

        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));

        assertThrows(AccessDeniedException.class, () ->
                facilityService.reactivateFacility(facility.getId(), provider.getEmail()));

        assertFalse(facility.isActive());
        verify(facilityRepository, never()).save(any());
    }

    @Test
    @DisplayName("Retrieve facilities owned by authenticated provider")
    void testGetFacilitiesByProvider_Success() {
        User provider = User.builder()
                .id(UUID.randomUUID())
                .email("provider@rescueroute.com")
                .role(UserRole.PROVIDER)
                .build();

        Facility facility1 = Facility.builder()
                .id(UUID.randomUUID())
                .name("Provider Facility 1")
                .facilityType("Hospital")
                .isActive(true)
                .createdBy(provider)
                .build();

        when(userRepository.findByEmail(provider.getEmail())).thenReturn(Optional.of(provider));
        when(facilityRepository.findByCreatedById(provider.getId())).thenReturn(List.of(facility1));

        List<FacilityResponseDto> result = facilityService.getFacilitiesByProvider(provider.getEmail());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Provider Facility 1", result.get(0).getName());
    }

    @Test
    @DisplayName("Admin can deactivate any approved facility (even if owned by provider)")
    void testAdminDeactivateFacility_Success() {
        User provider = User.builder()
                .id(UUID.randomUUID())
                .email("provider@rescueroute.com")
                .role(UserRole.PROVIDER)
                .build();

        User admin = User.builder()
                .id(UUID.randomUUID())
                .email("admin@rescueroute.com")
                .role(UserRole.ADMIN)
                .build();

        Facility facility = Facility.builder()
                .id(UUID.randomUUID())
                .name("Approved Hospital")
                .facilityType("Hospital")
                .isActive(true)
                .isVerified(true)
                .createdBy(provider)
                .build();

        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        when(verificationRequestRepository.findByFacilityIdAndStatus(facility.getId(), "PENDING"))
                .thenReturn(Collections.emptyList());
        when(facilityRepository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        facilityService.deactivateFacility(facility.getId(), admin.getEmail());

        assertFalse(facility.isActive(), "Facility must be deactivated by admin");
        assertTrue(facility.isVerified(), "Historical verification must be preserved");
        assertNotNull(facility.getDeletedAt(), "deletedAt must be recorded");

        ArgumentCaptor<AdminLog> logCaptor = ArgumentCaptor.forClass(AdminLog.class);
        verify(adminLogRepository).save(logCaptor.capture());
        assertEquals("FACILITY_DEACTIVATED", logCaptor.getValue().getActionType());
    }

    @Test
    @DisplayName("Admin can retrieve all facilities matching admin filters")
    void testGetAdminFacilities_Success() {
        Facility f1 = Facility.builder()
                .id(UUID.randomUUID())
                .name("Facility Active & Verified")
                .facilityType("Hospital")
                .isActive(true)
                .isVerified(true)
                .build();

        Facility f2 = Facility.builder()
                .id(UUID.randomUUID())
                .name("Facility Deactivated & Verified")
                .facilityType("Hospital")
                .isActive(false)
                .isVerified(true)
                .build();

        when(facilityRepository.findAll(ArgumentMatchers.<Specification<Facility>>any())).thenReturn(List.of(f1, f2));

        List<FacilityResponseDto> result = facilityService.getAdminFacilities("Hospital", "Karachi", null, true);

        assertNotNull(result);
        assertEquals(2, result.size());
    }
}
