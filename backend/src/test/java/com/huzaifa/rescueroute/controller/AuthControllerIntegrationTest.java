package com.huzaifa.rescueroute.controller;

import com.huzaifa.rescueroute.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

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
    @DisplayName("POST /api/v1/auth/register - Register new user successfully")
    void testRegister_Success() throws Exception {
        String uniqueEmail = "dr.ayesha." + System.currentTimeMillis() + "@rescueroute.local";
        String payload = String.format("""
                {
                    "fullName": "Dr. Ayesha Khan",
                    "email": "%s",
                    "password": "strongPassword123",
                    "role": "PROVIDER"
                }
                """, uniqueEmail);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value(uniqueEmail.toLowerCase()))
                .andExpect(jsonPath("$.data.user.role").value("PROVIDER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Reject duplicate email")
    void testRegister_DuplicateEmail() throws Exception {
        String existingEmail = "citizen@rescueroute.local";
        String payload = String.format("""
                {
                    "fullName": "Another Citizen",
                    "email": "%s",
                    "password": "password123"
                }
                """, existingEmail);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Reject invalid email format")
    void testRegister_InvalidEmail() throws Exception {
        String payload = """
                {
                    "fullName": "Invalid Email User",
                    "email": "not-an-email",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Reject self-promotion to ADMIN role")
    void testRegister_RejectAdminRole() throws Exception {
        String payload = """
                {
                    "fullName": "Fake Admin",
                    "email": "fake.admin@rescueroute.local",
                    "password": "password123",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("ADMIN is not permitted")));
    }

    @Test
    @DisplayName("Complete Auth Flow: Register -> Login -> GET /auth/me")
    void testAuthLifecycle() throws Exception {
        String uniqueEmail = "user." + System.currentTimeMillis() + "@rescueroute.local";
        String registerPayload = String.format("""
                {
                    "fullName": "Karachi Citizen",
                    "email": "%s",
                    "password": "mySecurePassword123"
                }
                """, uniqueEmail);

        // 1. Register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Login
        String loginPayload = String.format("""
                {
                    "email": "%s",
                    "password": "mySecurePassword123"
                }
                """, uniqueEmail);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andReturn();

        // Extract token
        String responseBody = loginResult.getResponse().getContentAsString();
        org.json.JSONObject json = new org.json.JSONObject(responseBody);
        String token = json.getJSONObject("data").getString("token");

        // 3. GET /api/v1/auth/me with valid Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(uniqueEmail.toLowerCase()))
                .andExpect(jsonPath("$.data.fullName").value("Karachi Citizen"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Reject wrong password with 401 Unauthorized")
    void testLogin_WrongPassword() throws Exception {
        // Register first
        String uniqueEmail = "wrongpw." + System.currentTimeMillis() + "@rescueroute.local";
        String registerPayload = String.format("""
                {
                    "fullName": "User",
                    "email": "%s",
                    "password": "correctPassword123"
                }
                """, uniqueEmail);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isCreated());

        String loginPayload = String.format("""
                {
                    "email": "%s",
                    "password": "incorrectPassword"
                }
                """, uniqueEmail);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Reject unauthenticated request with 401 Unauthorized")
    void testGetMe_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Reject invalid/tampered token with 401 Unauthorized")
    void testGetMe_InvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid.tampered.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
