package com.huzaifa.rescueroute.controller;

import com.huzaifa.rescueroute.entity.Facility;
import com.huzaifa.rescueroute.repository.FacilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class FacilityControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FacilityRepository facilityRepository;

    @BeforeEach
    void setUpDatabase() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        File seedFile = new File("../database/seed.sql");
        if (!seedFile.exists()) {
            seedFile = new File("database/seed.sql");
        }
        assertTrue(seedFile.exists(), "database/seed.sql must exist");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(seedFile));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }

    @Test
    @DisplayName("GET /api/v1/facilities - Retrieve all facilities")
    void testGetAllFacilities() throws Exception {
        mockMvc.perform(get("/api/v1/facilities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Facilities retrieved successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(9)));
    }

    @Test
    @DisplayName("GET /api/v1/facilities?type=Hospital - Filter by type")
    void testGetFacilitiesByType() throws Exception {
        mockMvc.perform(get("/api/v1/facilities")
                        .param("type", "Hospital")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", equalTo(3)))
                .andExpect(jsonPath("$.data[0].facilityType", equalToIgnoringCase("Hospital")));
    }

    @Test
    @DisplayName("GET /api/v1/facilities?city=Karachi - Filter by city")
    void testGetFacilitiesByCity() throws Exception {
        mockMvc.perform(get("/api/v1/facilities")
                        .param("city", "Karachi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(9)))
                .andExpect(jsonPath("$.data[0].city", equalToIgnoringCase("Karachi")));
    }

    @Test
    @DisplayName("GET /api/v1/facilities?active=true - Filter by active status")
    void testGetFacilitiesByActive() throws Exception {
        mockMvc.perform(get("/api/v1/facilities")
                        .param("active", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(9)))
                .andExpect(jsonPath("$.data[0].isActive", equalTo(true)));
    }

    @Test
    @DisplayName("GET /api/v1/facilities/{id} - Retrieve single facility with subtype details")
    void testGetFacilityById_Found() throws Exception {
        List<Facility> hospitals = facilityRepository.findByFacilityType("Hospital");
        assertFalse(hospitals.isEmpty(), "At least one hospital must exist");
        Facility targetHospital = hospitals.get(0);

        mockMvc.perform(get("/api/v1/facilities/" + targetHospital.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Facility retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(targetHospital.getId().toString()))
                .andExpect(jsonPath("$.data.name").value(targetHospital.getName()))
                .andExpect(jsonPath("$.data.facilityType").value("Hospital"))
                .andExpect(jsonPath("$.data.hospitalDetails").exists())
                .andExpect(jsonPath("$.data.hospitalDetails.emergencyAvailable").isBoolean());
    }

    @Test
    @DisplayName("GET /api/v1/facilities/{id} - Return 404 when facility does not exist")
    void testGetFacilityById_NotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/facilities/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Facility not found with ID: " + nonExistentId)));
    }

    @Test
    @DisplayName("GET /api/v1/facilities/search - Successful Top-K ranked facility retrieval")
    void testSearchFacilities_Success() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/search")
                        .param("lat", "24.8607")
                        .param("lng", "67.0011")
                        .param("k", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Ranked facilities retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].score").isNumber())
                .andExpect(jsonPath("$.data[0].distanceKm").isNumber())
                .andExpect(jsonPath("$.data[0].facility").exists())
                .andExpect(jsonPath("$.data[0].facility.name").isString());
    }

    @Test
    @DisplayName("GET /api/v1/facilities/search - Filter by type and emergency mode")
    void testSearchFacilities_WithFilters() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/search")
                        .param("lat", "24.8607")
                        .param("lng", "67.0011")
                        .param("type", "Hospital")
                        .param("emergency", "true")
                        .param("k", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data[0].facility.facilityType").value("Hospital"));
    }

    @Test
    @DisplayName("GET /api/v1/facilities/search - Reject out-of-range coordinates with 400 Bad Request")
    void testSearchFacilities_InvalidCoordinates() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/search")
                        .param("lat", "95.0")
                        .param("lng", "67.0011")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("latitude must be between -90.0 and 90.0")));
    }

    @Test
    @DisplayName("GET /api/v1/facilities/search - Missing required coordinates returns 400 Bad Request")
    void testSearchFacilities_MissingCoordinates() throws Exception {
        mockMvc.perform(get("/api/v1/facilities/search")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

