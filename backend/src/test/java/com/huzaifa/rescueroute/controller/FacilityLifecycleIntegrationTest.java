package com.huzaifa.rescueroute.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huzaifa.rescueroute.common.security.JwtService;
import com.huzaifa.rescueroute.dto.CreateFacilityRequestDto;
import com.huzaifa.rescueroute.dto.HospitalDetailsDto;
import com.huzaifa.rescueroute.dto.UpdateFacilityRequestDto;
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
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class FacilityLifecycleIntegrationTest {

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
    private String otherProviderToken;
    private String adminToken;

    private User providerUser;
    private User otherProviderUser;
    private User adminUser;

    @AfterEach
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

        providerUser = userRepository.findByEmail("provider.lifecycle@rescueroute.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Dr. Lifecycle Provider")
                        .email("provider.lifecycle@rescueroute.local")
                        .passwordHash(passwordEncoder.encode("Password123!"))
                        .role(UserRole.PROVIDER)
                        .isActive(true)
                        .build()));

        otherProviderUser = userRepository.findByEmail("other.provider@rescueroute.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName("Dr. Another Provider")
                        .email("other.provider@rescueroute.local")
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

        providerToken = "Bearer " + jwtService.generateToken(providerUser.getEmail(), providerUser.getId(), providerUser.getRole());
        otherProviderToken = "Bearer " + jwtService.generateToken(otherProviderUser.getEmail(), otherProviderUser.getId(), otherProviderUser.getRole());
        adminToken = "Bearer " + jwtService.generateToken(adminUser.getEmail(), adminUser.getId(), adminUser.getRole());
    }

    private void cleanupTestData() {
        try {
            List<Facility> testFacilities = facilityRepository.findAll().stream()
                    .filter(f -> f.getName() != null && f.getName().startsWith("Phase3 Lifecycle"))
                    .toList();
            for (Facility f : testFacilities) {
                List<AdminLog> logs = adminLogRepository.findByTargetEntityId(f.getId());
                adminLogRepository.deleteAll(logs);
                List<VerificationRequest> requests = verificationRequestRepository.findByFacilityId(f.getId());
                verificationRequestRepository.deleteAll(requests);
                facilityRepository.delete(f);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Complete Phase 3 Lifecycle: Create -> Verify -> Update (Staged) -> IDOR Check -> Admin Approve -> Deactivate (Soft-delete) -> Reactivate")
    void testCompletePhase3FacilityLifecycle() throws Exception {
        // Step 1: Provider creates facility
        CreateFacilityRequestDto createDto = CreateFacilityRequestDto.builder()
                .name("Phase3 Lifecycle Hospital")
                .facilityType("Hospital")
                .address("Block 5, Clifton, Karachi")
                .city("Karachi")
                .latitude(24.8150)
                .longitude(67.0320)
                .phoneNumber("021-35800000")
                .hospitalDetails(HospitalDetailsDto.builder()
                        .emergencyAvailable(true)
                        .icuAvailable(false)
                        .traumaCenterAvailable(false)
                        .notes("Initial hospital launch")
                        .build())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/facilities")
                        .header("Authorization", providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name", is("Phase3 Lifecycle Hospital")))
                .andExpect(jsonPath("$.data.isVerified", is(false)))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID facilityId = UUID.fromString(createdJson.path("data").path("id").asText());

        // Locate pending creation verification request
        List<VerificationRequest> initialRequests = verificationRequestRepository.findByFacilityIdAndStatus(facilityId, "PENDING");
        assertEquals(1, initialRequests.size());
        UUID creationReqId = initialRequests.get(0).getId();

        // Step 2: Admin approves creation
        VerificationDecisionRequestDto approveDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .reviewNotes("Facility credentials verified")
                .build();

        mockMvc.perform(put("/api/v1/admin/verification-requests/{id}", creationReqId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // Verify facility is now verified
        Facility verifiedFacility = facilityRepository.findById(facilityId).orElseThrow();
        assertTrue(verifiedFacility.isVerified());
        assertFalse(verifiedFacility.isHasPendingUpdate());

        // Step 3: Provider submits update (staged changes)
        UpdateFacilityRequestDto updateDto = UpdateFacilityRequestDto.builder()
                .name("Phase3 Lifecycle Hospital & Trauma Center")
                .address("Block 5, Clifton Main Road")
                .city("Karachi")
                .latitude(24.8155)
                .longitude(67.0325)
                .phoneNumber("021-35899999")
                .hospitalDetails(HospitalDetailsDto.builder()
                        .emergencyAvailable(true)
                        .icuAvailable(true)
                        .traumaCenterAvailable(true)
                        .notes("Upgraded to full trauma center")
                        .build())
                .build();

        mockMvc.perform(put("/api/v1/facilities/{id}", facilityId)
                        .header("Authorization", providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasPendingUpdate", is(true)));

        // Verify that live public facility directory has NOT been modified yet
        mockMvc.perform(get("/api/v1/facilities/{id}", facilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Phase3 Lifecycle Hospital"))) // still original name!
                .andExpect(jsonPath("$.data.hasPendingUpdate", is(true)));

        // Step 4: IDOR check - other provider attempts to update this facility
        mockMvc.perform(put("/api/v1/facilities/{id}", facilityId)
                        .header("Authorization", otherProviderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden());

        // Step 5: Admin inspects verification queue and approves the UPDATE request
        List<VerificationRequest> updateRequests = verificationRequestRepository.findByFacilityIdAndStatus(facilityId, "PENDING");
        assertEquals(1, updateRequests.size());
        VerificationRequest updateReq = updateRequests.get(0);
        assertEquals("UPDATE", updateReq.getRequestType());
        assertNotNull(updateReq.getProposedChanges());

        VerificationDecisionRequestDto approveUpdateDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .reviewNotes("Trauma center upgrades verified on-site")
                .build();

        mockMvc.perform(put("/api/v1/admin/verification-requests/{id}", updateReq.getId())
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveUpdateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // Live public directory now reflects the approved updates
        mockMvc.perform(get("/api/v1/facilities/{id}", facilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Phase3 Lifecycle Hospital & Trauma Center")))
                .andExpect(jsonPath("$.data.address", is("Block 5, Clifton Main Road")))
                .andExpect(jsonPath("$.data.hasPendingUpdate", is(false)))
                .andExpect(jsonPath("$.data.isVerified", is(true)));

        // Step 6: Unauthorized deactivation check (other provider tries to delete)
        mockMvc.perform(delete("/api/v1/facilities/{id}", facilityId)
                        .header("Authorization", otherProviderToken))
                .andExpect(status().isForbidden());

        // Step 7: Authorized provider soft-deletes the facility
        mockMvc.perform(delete("/api/v1/facilities/{id}", facilityId)
                        .header("Authorization", providerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive", is(false)))
                .andExpect(jsonPath("$.data.deletedAt", notNullValue()));

        // Step 8: Public lookup by ID returns 404 (soft-deleted exclusion)
        mockMvc.perform(get("/api/v1/facilities/{id}", facilityId))
                .andExpect(status().isNotFound());

        // Public discovery list excludes the deactivated facility
        mockMvc.perform(get("/api/v1/facilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(facilityId.toString()))));

        // Step 9: Unauthorized user attempts to reactivate (provider cannot reactivate)
        mockMvc.perform(post("/api/v1/facilities/{id}/reactivate", facilityId)
                        .header("Authorization", providerToken))
                .andExpect(status().isForbidden());

        // Step 10: Admin reactivates facility
        mockMvc.perform(post("/api/v1/facilities/{id}/reactivate", facilityId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andExpect(jsonPath("$.data.deletedAt").doesNotExist());

        // Public lookup now succeeds again
        mockMvc.perform(get("/api/v1/facilities/{id}", facilityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Phase3 Lifecycle Hospital & Trauma Center")));
    }

    @Test
    @DisplayName("Admin can directly deactivate approved facility owned by provider and lifecycle status is reflected in admin endpoints")
    void testAdminDeactivateFacilityOwnedByProvider_AndVerifyLifecycleReflection() throws Exception {
        // Step 1: Provider creates facility
        CreateFacilityRequestDto createDto = CreateFacilityRequestDto.builder()
                .name("Phase3 Lifecycle AdminManaged Clinic")
                .facilityType("Clinic")
                .address("Shahrah-e-Faisal, Karachi")
                .city("Karachi")
                .latitude(24.8600)
                .longitude(67.0700)
                .phoneNumber("021-34500000")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/facilities")
                        .header("Authorization", providerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID facilityId = UUID.fromString(createdJson.path("data").path("id").asText());

        List<VerificationRequest> requests = verificationRequestRepository.findByFacilityIdAndStatus(facilityId, "PENDING");
        assertEquals(1, requests.size());
        UUID reqId = requests.get(0).getId();

        // Step 2: Admin approves creation
        VerificationDecisionRequestDto approveDto = VerificationDecisionRequestDto.builder()
                .decision("APPROVED")
                .reviewNotes("Verified clinic")
                .build();

        mockMvc.perform(put("/api/v1/admin/verification-requests/{id}", reqId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk());

        // Step 3: Admin directly deactivates the approved facility
        mockMvc.perform(delete("/api/v1/facilities/{id}", facilityId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive", is(false)))
                .andExpect(jsonPath("$.data.isVerified", is(true))) // verification retained!
                .andExpect(jsonPath("$.data.deletedAt", notNullValue()));

        // Step 4: Verification request query in admin moderation queue now reflects facilityActive = false
        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .header("Authorization", adminToken)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.facilityId == '" + facilityId + "')].facilityActive", hasItem(false)))
                .andExpect(jsonPath("$.data[?(@.facilityId == '" + facilityId + "')].facilityDeletedAt", notNullValue()));

        // Step 5: Admin reactivates facility
        mockMvc.perform(post("/api/v1/facilities/{id}/reactivate", facilityId)
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andExpect(jsonPath("$.data.isVerified", is(true)));

        // Moderation queue now reflects facilityActive = true
        mockMvc.perform(get("/api/v1/admin/verification-requests")
                        .header("Authorization", adminToken)
                        .param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.facilityId == '" + facilityId + "')].facilityActive", hasItem(true)));
    }

    @Test
    @DisplayName("Admin facilities endpoint security and filtering")
    void testAdminGetFacilitiesEndpoint_SecurityAndFilters() throws Exception {
        // Unauthenticated request receives 401 or 403
        mockMvc.perform(get("/api/v1/facilities/admin"))
                .andExpect(status().isForbidden());

        // Provider role request receives 403 Forbidden
        mockMvc.perform(get("/api/v1/facilities/admin")
                        .header("Authorization", providerToken))
                .andExpect(status().isForbidden());

        // Admin role request succeeds
        mockMvc.perform(get("/api/v1/facilities/admin")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").isArray());

        // Admin filter active=true
        mockMvc.perform(get("/api/v1/facilities/admin")
                        .header("Authorization", adminToken)
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Admin filter active=false
        mockMvc.perform(get("/api/v1/facilities/admin")
                        .header("Authorization", adminToken)
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}

