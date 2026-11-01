package com.huzaifa.rescueroute.service;

import com.huzaifa.rescueroute.dto.ClinicDetailsDto;
import com.huzaifa.rescueroute.dto.CreateFacilityRequestDto;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.entity.Clinic;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.service.impl.RankingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ClinicMappingAndWorkflowTest {

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

    private FacilityMapper facilityMapper;
    private RankingServiceImpl rankingService;

    @BeforeEach
    void setUp() {
        facilityMapper = new FacilityMapper();
        rankingService = new RankingServiceImpl(
                facilityRepository,
                hospitalRepository,
                bloodBankRepository,
                pharmacyRepository,
                ambulanceRepository,
                clinicRepository,
                facilityMapper
        );
    }

    @Test
    @DisplayName("FacilityMapper maps Clinic entity to ClinicDetailsDto correctly")
    void testFacilityMapper_MapsClinicEntityToDto() {
        UUID facilityId = UUID.randomUUID();
        UUID clinicId = UUID.randomUUID();

        Facility facility = Facility.builder()
                .id(facilityId)
                .name("Al-Khidmat Clinic")
                .facilityType("Clinic")
                .address("Gulshan Block 6")
                .city("Karachi")
                .latitude(24.9180)
                .longitude(67.0971)
                .isVerified(true)
                .isActive(true)
                .build();

        Clinic clinic = Clinic.builder()
                .id(clinicId)
                .facility(facility)
                .consultationAvailable(true)
                .specialties("General Physician, Pediatrics")
                .openWeekends(true)
                .notes("OPD consultation active")
                .build();

        facility.setClinic(clinic);

        FacilityResponseDto dto = facilityMapper.toDto(facility);

        assertNotNull(dto);
        assertEquals("Al-Khidmat Clinic", dto.getName());
        assertEquals("Clinic", dto.getFacilityType());
        assertNotNull(dto.getClinicDetails());
        assertEquals(clinicId, dto.getClinicDetails().getId());
        assertTrue(dto.getClinicDetails().isConsultationAvailable());
        assertEquals("General Physician, Pediatrics", dto.getClinicDetails().getSpecialties());
        assertTrue(dto.getClinicDetails().isOpenWeekends());
        assertEquals("OPD consultation active", dto.getClinicDetails().getNotes());
    }

    @Test
    @DisplayName("RankingServiceImpl checks emergency availability for Clinic")
    void testRankingService_ClinicAvailability() {
        Facility openWeekendClinic = Facility.builder()
                .facilityType("Clinic")
                .build();
        openWeekendClinic.setClinic(Clinic.builder()
                .consultationAvailable(true)
                .openWeekends(true)
                .build());

        Facility weekdayOnlyClinic = Facility.builder()
                .facilityType("Clinic")
                .build();
        weekdayOnlyClinic.setClinic(Clinic.builder()
                .consultationAvailable(true)
                .openWeekends(false)
                .build());

        assertTrue(rankingService.checkEmergencyAvailability(openWeekendClinic));
        assertFalse(rankingService.checkEmergencyAvailability(weekdayOnlyClinic));
    }

    @Test
    @DisplayName("CreateFacilityRequestDto accepts Clinic facility type")
    void testCreateFacilityRequestDto_AcceptsClinic() {
        CreateFacilityRequestDto dto = CreateFacilityRequestDto.builder()
                .name("Test Clinic")
                .facilityType("Clinic")
                .address("Test Address")
                .city("Karachi")
                .latitude(24.9000)
                .longitude(67.1000)
                .clinicDetails(ClinicDetailsDto.builder()
                        .consultationAvailable(true)
                        .specialties("Dermatology")
                        .openWeekends(false)
                        .build())
                .build();

        assertEquals("Clinic", dto.getFacilityType());
        assertNotNull(dto.getClinicDetails());
        assertEquals("Dermatology", dto.getClinicDetails().getSpecialties());
    }
}
