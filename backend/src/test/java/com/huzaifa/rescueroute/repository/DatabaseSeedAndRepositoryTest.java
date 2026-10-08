package com.huzaifa.rescueroute.repository;

import com.huzaifa.rescueroute.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DatabaseSeedAndRepositoryTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private BloodBankRepository bloodBankRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Test
    @DisplayName("Verify database schema and seed data via Spring Data JPA repositories")
    void testSeedDataAndRepositories() {
        // Execute database/seed.sql
        File seedFile = new File("../database/seed.sql");
        if (!seedFile.exists()) {
            seedFile = new File("database/seed.sql");
        }
        assertTrue(seedFile.exists(), "database/seed.sql must exist");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(seedFile));
        populator.setContinueOnError(false);
        populator.execute(dataSource);

        // 1. Verify Users
        assertTrue(userRepository.count() >= 3, "Should have at least 3 users");
        Optional<User> admin = userRepository.findByEmail("admin@rescueroute.local");
        assertTrue(admin.isPresent(), "Admin user should exist");
        assertEquals(UserRole.ADMIN, admin.get().getRole());

        // 2. Verify Facilities
        List<Facility> facilities = facilityRepository.findAll();
        assertTrue(facilities.size() >= 9, "Should have at least 9 facilities in Karachi");

        // 3. Verify Hospitals
        List<Facility> hospitals = facilityRepository.findByFacilityType("Hospital");
        assertEquals(3, hospitals.size(), "Should have 3 hospitals");
        List<Hospital> emergencyHospitals = hospitalRepository.findByEmergencyAvailableTrue();
        assertFalse(emergencyHospitals.isEmpty(), "Should have emergency-ready hospitals");
        for (Hospital h : emergencyHospitals) {
            assertNotNull(h.getFacility(), "Hospital must be linked to its parent facility");
            assertNotNull(h.getFacility().getName());
        }

        // 4. Verify Blood Banks
        List<Facility> bloodBanks = facilityRepository.findByFacilityType("Blood Bank");
        assertEquals(2, bloodBanks.size(), "Should have 2 blood banks");

        // 5. Verify Pharmacies
        List<Facility> pharmacies = facilityRepository.findByFacilityType("Pharmacy");
        assertEquals(2, pharmacies.size(), "Should have 2 pharmacies");
        List<Pharmacy> open24Hours = pharmacyRepository.findByOpen24HoursTrue();
        assertFalse(open24Hours.isEmpty(), "Should have 24/7 emergency pharmacies");

        // 6. Verify Ambulances
        List<Facility> ambulances = facilityRepository.findByFacilityType("Ambulance");
        assertEquals(2, ambulances.size(), "Should have 2 ambulance services");
        List<Ambulance> availableAmbulances = ambulanceRepository.findByAvailableNowTrue();
        assertFalse(availableAmbulances.isEmpty(), "Should have available ambulance units");
    }
}
