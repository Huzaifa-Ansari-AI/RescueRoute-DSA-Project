package com.huzaifa.rescueroute.service.impl;

import com.huzaifa.rescueroute.common.exception.ResourceNotFoundException;
import com.huzaifa.rescueroute.dto.FacilityResponseDto;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.mapper.FacilityMapper;
import com.huzaifa.rescueroute.repository.*;
import com.huzaifa.rescueroute.repository.specification.FacilitySpecifications;
import com.huzaifa.rescueroute.service.FacilityService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of FacilityService.
 */
@Service
@Transactional(readOnly = true)
public class FacilityServiceImpl implements FacilityService {

    private final FacilityRepository facilityRepository;
    private final HospitalRepository hospitalRepository;
    private final BloodBankRepository bloodBankRepository;
    private final PharmacyRepository pharmacyRepository;
    private final AmbulanceRepository ambulanceRepository;
    private final FacilityMapper facilityMapper;

    public FacilityServiceImpl(
            FacilityRepository facilityRepository,
            HospitalRepository hospitalRepository,
            BloodBankRepository bloodBankRepository,
            PharmacyRepository pharmacyRepository,
            AmbulanceRepository ambulanceRepository,
            FacilityMapper facilityMapper) {
        this.facilityRepository = facilityRepository;
        this.hospitalRepository = hospitalRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.ambulanceRepository = ambulanceRepository;
        this.facilityMapper = facilityMapper;
    }

    @Override
    public List<FacilityResponseDto> getAllFacilities(String type, String city, Boolean active) {
        Specification<Facility> spec = FacilitySpecifications.withFilters(type, city, active);
        List<Facility> facilities = facilityRepository.findAll(spec);
        return facilities.stream()
                .map(this::enrichAndMapFacility)
                .collect(Collectors.toList());
    }

    @Override
    public FacilityResponseDto getFacilityById(UUID id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with ID: " + id));
        return enrichAndMapFacility(facility);
    }

    /**
     * Ensures subtype details are loaded from respective repositories if lazy loading was not initialized,
     * then converts the entity into FacilityResponseDto.
     */
    private FacilityResponseDto enrichAndMapFacility(Facility facility) {
        if (facility == null) {
            return null;
        }

        if (facility.getHospital() == null && "Hospital".equalsIgnoreCase(facility.getFacilityType())) {
            hospitalRepository.findByFacilityId(facility.getId()).ifPresent(facility::setHospital);
        } else if (facility.getBloodBank() == null && "Blood Bank".equalsIgnoreCase(facility.getFacilityType())) {
            bloodBankRepository.findByFacilityId(facility.getId()).ifPresent(facility::setBloodBank);
        } else if (facility.getPharmacy() == null && "Pharmacy".equalsIgnoreCase(facility.getFacilityType())) {
            pharmacyRepository.findByFacilityId(facility.getId()).ifPresent(facility::setPharmacy);
        } else if (facility.getAmbulance() == null && "Ambulance".equalsIgnoreCase(facility.getFacilityType())) {
            ambulanceRepository.findByFacilityId(facility.getId()).ifPresent(facility::setAmbulance);
        }

        return facilityMapper.toDto(facility);
    }
}
