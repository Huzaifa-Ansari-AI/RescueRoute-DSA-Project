package com.huzaifa.rescueroute.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.common.security.JwtService;
import com.huzaifa.rescueroute.dto.CreateFacilityRequestDto;
import com.huzaifa.rescueroute.dto.HospitalDetailsDto;
import com.huzaifa.rescueroute.dto.VerificationDecisionRequestDto;
import com.huzaifa.rescueroute.entity.AdminLog;
import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.entity.User;
import com.huzaifa.rescueroute.entity.UserRole;
import com.huzaifa.rescueroute.entity.VerificationRequest;
import com.huzaifa.rescueroute.repository.AdminLogRepository;
import com.huzaifa.rescueroute.repository.FacilityRepository;
import com.huzaifa.rescueroute.repository.UserRepository;
import com.huzaifa.rescueroute.repository.VerificationRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class VerificationWorkflowFullLifecycleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private VerificationRequestRepository verificationRequestRepository;

    @Autowired
    private AdminLogRepository adminLogRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String providerToken;
    private String adminToken;
    private String userToken;

    private User providerUser;
    private User adminUser;
    private User regularUser;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        cleanupTestData();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        cleanupTestData();

        File seedFile = new File("../database/seed.sql");
        if (!seedFile.exists()) {
            seedFile = new File("database/seed.sql");
        }
        assertTrue(seedFile.exists(), "database/seed.sql must exist");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(seedFile));
        populator.setContinueOnError(true);
        populator.execute(dataSource);

        // Fetch or create testing users
        providerUser = userRepository.findByEmail("provider@rescueroute.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Karachi Healthcare Provider")
                        .email("provider@rescueroute.local")
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(UserRole.PROVIDER)
                        .isActive(true)
                        .build()));

        adminUser = userRepository.findByEmail("admin@rescueroute.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("RescueRoute Admin")
                        .email("admin@rescueroute.local")
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(UserRole.ADMIN)
                        .isActive(true)
                        .build()));

        regularUser = userRepository.findByEmail("citizen@rescueroute.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Public Citizen")
                        .email("citizen@rescueroute.local")
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(UserRole.USER)
                        .isActive(true)
                        .build()));

        providerToken = "Bearer " + jwtService.generateToken(providerUser.getEmail(), providerUser.getId(), providerUser.getRole());
        adminToken = "Bearer " + jwtService.generateToken(adminUser.getEmail(), adminUser.getId(), adminUser.getRole());
        userToken = "Bearer " + jwtService.generateToken(regularUser.getEmail(), regularUser.getId(), regularUser.getRole());
    }

    private void cleanupTestData() {
        try {
            List<Facility> testFacilities = facilityRepository.findAll().stream()
                    .filter(f -> f.getName() != null && (f.getName().startsWith("Clifton Surgical Hospital") || f.getName().startsWith("Unverified Clinic")))
                    .toList();
            for (Facility f : testFacilities) {
                List<AdminLog> logs = adminLogRepository.findByTargetEntityId(f.getId());
                adminLogRepository.deleteAll(logs);
                List<VerificationRequest> requests = verificationRequestRepository.findByFacilityId(f.getId());
                verificationRequestRepository.deleteAll(requests);
                facilityRepository.delete(f);
            }
        } catch (Exception e) {
            // Ignore cleanup failures on setup
        }
    }

    @Test
    @DisplayName("Complete Lifecycle: Provider submits facility -> Unverified -> Verification Request -> Admin Approves -> Facility Verified -> Admin Log created -> Ranking boosted")
    void testCompleteVerificationApprovalLifecycle() throws Exception {
        // Step 1: Provider submits facility
        String uniqueName = "Clifton Surgical Hospital - " + UUID.randomUUID().toString().substring(0, 8);
        CreateFacilityRequestDto createDto = CreateFacilityRequestDto.builder()
                .name(uniqueName)
                .facilityType("Hospital")
                .address("Block 5, Clifton, Karachi")
                .city("Karachi")
                .latitude(24.8235)
                .longitude(67.0312)
                .phoneNumber("021-35870000")
                .hospitalDetails(HospitalDetailsDto.builder()
                        .emergencyAvailable(true)
                        .icuAvailable(true)
                        .traumaCenterAvailable(true)
                        .notes("Tertiary emergency care unit")
                        .build())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/facilities")
                        .header("Authorization", providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value(uniqueName))
                .andExpect(jsonPath("$.data.isVerified").value(false))
                .andReturn();

        String responseJson = createResult.getResponse().getContentAsString();
        String facilityIdStr = objectMapper.readTree(responseJson).path("data").path("id").asText();
        assertNotNull(facilityIdStr);
        UUID facilityId = UUID.fromString(facilityIdStr);

        // Verify database state: facility is unverified
        Facility createdFacility = facilityRepository.findById(facilityId).orElseThrow();
        assertFalse(createdFacility.isVerified(), "Newly created facility must strictly be unverified");
        assertEquals("Karachi", createdFacility.getCity());

        // Verify verification request auto-created in PENDING status
        List<VerificationRequest> requests = verificationRequestRepository.findByFacilityId(facilityId);
        assertFalse(requests.isEmpty(), "Verification request must automatically be created");
        VerificationRequest verificationRequest = requests.get(0);
        assertEquals("PENDING", verificationRequest.getStatus());
        assertEquals(providerUser.getId(), verificationRequest.getSubmittedBy().getId());

        // Step 2: Unauthenticated user cannot create facility
        mockMvc.perform(post("/api/v1/facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isUnauthorized());

        // Step 3: Regular USER cannot create facility
        mockMvc.perform(post("/api/v1/facilities")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isForbidden());

        // Step 4: Provider cannot access Admin moderation queue
        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .header("Authorization", providerToken))
                .andExpect(status().isForbidden());

        // Step 5: Admin retrieves pending verification requests
        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .header("Authorization", adminToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        // Step 6: Admin APPROVES the request
        VerificationDecisionRequestDto approveDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .reviewNotes("Verified with Sindh Healthcare Commission documentation")
                .build();

        mockMvc.perform(put("/api/v1/admin/verification-requests/" + verificationRequest.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.facilityVerified").value(true));

        // Step 7: Verify database state after approval
        Facility approvedFacility = facilityRepository.findById(facilityId).orElseThrow();
        assertTrue(approvedFacility.isVerified(), "Facility must now have is_verified = true");

        VerificationRequest updatedRequest = verificationRequestRepository.findById(verificationRequest.getId()).orElseThrow();
        assertEquals("APPROVED", updatedRequest.getStatus());
        assertEquals(adminUser.getId(), updatedRequest.getReviewedBy().getId());
        assertNotNull(updatedRequest.getReviewedAt());

        // Step 8: Verify admin audit log created
        List<AdminLog> adminLogs = adminLogRepository.findByTargetEntityId(facilityId);
        assertFalse(adminLogs.isEmpty(), "Admin log must be created for verification approval");
        AdminLog log = adminLogs.get(0);
        assertEquals("VERIFICATION_APPROVED", log.getActionType());
        assertEquals(adminUser.getId(), log.getAdmin().getId());

        // Step 9: Re-processing already approved request returns 409 Conflict
        mockMvc.perform(put("/api/v1/admin/verification-requests/" + verificationRequest.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // Step 10: Verify ranking endpoint observes verified status
        mockMvc.perform(get("/api/v1/facilities/search")
                        .param("lat", "24.8235")
                        .param("lng", "67.0312")
                        .param("type", "Hospital")
                        .param("k", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[?(@.facility.id == '" + facilityId + "')].isVerified", hasItem(true)));
    }

    @Test
    @DisplayName("Lifecycle Rejection: Provider submits -> Admin Rejects with notes -> Facility remains unverified -> Admin Log created")
    void testVerificationRejectionLifecycle() throws Exception {
        String uniqueName = "Unverified Clinic - " + UUID.randomUUID().toString().substring(0, 8);
        CreateFacilityRequestDto createDto = CreateFacilityRequestDto.builder()
                .name(uniqueName)
                .facilityType("Pharmacy")
                .address("Shahrah-e-Liaquat")
                .city("Karachi")
                .latitude(24.8550)
                .longitude(67.0100)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/facilities")
                        .header("Authorization", providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        String facilityIdStr = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
        UUID facilityId = UUID.fromString(facilityIdStr);

        VerificationRequest verificationRequest = verificationRequestRepository.findByFacilityId(facilityId).get(0);

        // Rejection without notes -> 400 Bad Request
        VerificationDecisionRequestDto rejectWithoutNotes = VerificationDecisionRequestDto.builder()
                .decision("REJECTED")
                .reviewNotes("  ")
                .build();

        mockMvc.perform(put("/api/v1/admin/verification-requests/" + verificationRequest.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectWithoutNotes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Rejection with proper notes -> 200 OK
        VerificationDecisionRequestDto validReject = VerificationDecisionRequestDto.builder()
                .decision("REJECTED")
                .reviewNotes("Pharmacy license expired")
                .build();

        mockMvc.perform(put("/api/v1/admin/verification-requests/" + verificationRequest.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.facilityVerified").value(false));

        // Facility must remain unverified
        Facility rejectedFacility = facilityRepository.findById(facilityId).orElseThrow();
        assertFalse(rejectedFacility.isVerified(), "Rejected facility must remain unverified");

        // Admin log created
        List<AdminLog> adminLogs = adminLogRepository.findByTargetEntityId(facilityId);
        assertFalse(adminLogs.isEmpty());
        assertEquals("VERIFICATION_REJECTED", adminLogs.get(0).getActionType());
    }

    @Test
    @DisplayName("Non-existent verification request returns 404 Not Found")
    void testNonExistentVerificationRequestReturns404() throws Exception {
        UUID randomId = UUID.randomUUID();
        VerificationDecisionRequestDto dto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .build();

        mockMvc.perform(put("/api/v1/admin/verification-requests/" + randomId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
