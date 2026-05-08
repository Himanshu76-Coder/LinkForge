package com.linkforge.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkforge.urlshortener.dto.request.ChangePasswordRequest;
import com.linkforge.urlshortener.dto.request.CreateUrlRequest;
import com.linkforge.urlshortener.dto.request.LoginRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.request.UpdateProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for UserController - uses H2 in-memory DB
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("john_doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    // ==========================================
    // GET /api/v1/users/me
    // ==========================================

    @Test
    void getProfile_withValidAuth_returns200WithProfileData() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("john_doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.hashedPassword").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void getProfile_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // PUT /api/v1/users/me
    // ==========================================

    @Test
    void updateProfile_withValidData_returns200WithUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("updated@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("updated@example.com"));
    }

    @Test
    void updateProfile_withSameUsername_returns200WithoutError() throws Exception {
        // Sending the same username that the user already has must succeed silently
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("john_doe");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("john_doe"));
    }

    @Test
    void updateProfile_withTakenEmail_returns409() throws Exception {
        // Register another user
        RegisterRequest anotherUser = new RegisterRequest();
        anotherUser.setUsername("another_user");
        anotherUser.setEmail("another@example.com");
        anotherUser.setPassword("password123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(anotherUser)));

        // Try to update to the taken email
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("another@example.com");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void updateProfile_withNoFields_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================
    // PATCH /api/v1/users/me/password
    // ==========================================

    @Test
    void changePassword_withCorrectCurrentPassword_returns200() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("newpassword456");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void changePassword_withWrongCurrentPassword_returns401() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongpassword");
        request.setNewPassword("newpassword456");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void changePassword_withSamePassword_returns400() throws Exception {
        // Sending the same password as the current one must be rejected as a business rule
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("password123");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void changePassword_withShortNewPassword_returns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("password123");
        request.setNewPassword("short");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    // ==========================================
    // GET /api/v1/users/me/stats
    // ==========================================

    @Test
    void getStats_withValidAuth_returns200WithAllAggregateFields() throws Exception {
        // Create a URL so stats are non-trivially populated
        CreateUrlRequest urlRequest = new CreateUrlRequest();
        urlRequest.setOriginalUrl("https://www.example.com");
        mockMvc.perform(post("/api/v1/urls")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(urlRequest)));

        mockMvc.perform(get("/api/v1/users/me/stats")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUrls").value(1))
                .andExpect(jsonPath("$.data.totalClicks").value(0))
                .andExpect(jsonPath("$.data.activeUrls").value(1))
                .andExpect(jsonPath("$.data.inactiveUrls").value(0))
                .andExpect(jsonPath("$.data.expiredUrls").value(0));
    }

    // ==========================================
    // DELETE /api/v1/users/me
    // ==========================================

    @Test
    void deleteAccount_withValidAuth_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAccount_thenAccessWithOldToken_returns401() throws Exception {
        // Delete the account
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // The JWT filter will try to load the user by ID, find it deleted,
        // and leave the security context empty - resulting in 401
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }
}
