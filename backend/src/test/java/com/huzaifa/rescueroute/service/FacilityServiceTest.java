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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                facilityMapper
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

        when(facilityRepository.findAll(any(Specification.class))).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Aga Khan University Hospital", result.get(0).getName());
        assertEquals("Hospital", result.get(0).getFacilityType());
        assertTrue(result.get(0).isVerified());
        assertTrue(result.get(0).isActive());
        verify(facilityRepository, times(1)).findAll(any(Specification.class));
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

        when(facilityRepository.findAll(any(Specification.class))).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities("Hospital", null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Indus Hospital", result.get(0).getName());
        verify(facilityRepository, times(1)).findAll(any(Specification.class));
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

        when(facilityRepository.findAll(any(Specification.class))).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities(null, "Karachi", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Karachi", result.get(0).getCity());
        verify(facilityRepository, times(1)).findAll(any(Specification.class));
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

        when(facilityRepository.findAll(any(Specification.class))).thenReturn(List.of(facility));

        List<FacilityResponseDto> result = facilityService.getAllFacilities(null, null, true);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
        verify(facilityRepository, times(1)).findAll(any(Specification.class));
    }

    @Test
    @DisplayName("Filter facilities by combined criteria (type, city, active)")
    void testGetAllFacilities_CombinedFilters() {
        when(facilityRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        List<FacilityResponseDto> result = facilityService.getAllFacilities("Ambulance", "Karachi", true);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(facilityRepository, times(1)).findAll(any(Specification.class));
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
}
