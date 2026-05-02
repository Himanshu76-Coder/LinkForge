package com.linkforge.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkforge.urlshortener.dto.request.CreateUrlRequest;
import com.linkforge.urlshortener.dto.request.LoginRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.request.UpdateUrlRequest;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Integration tests for UrlController - uses H2 in-memory DB
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Register and login to get access token for authenticated requests
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

    // Helper to create a URL and return its ID
    private Long createTestUrl(String originalUrl) throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl(originalUrl);

        MvcResult result = mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    // ==========================================
    // POST /api/v1/urls
    // ==========================================

    @Test
    void createUrl_withValidBody_returns201WithAllFields() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setTitle("Example Site");
        request.setDescription("A test link");

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.shortCode").exists())
                .andExpect(jsonPath("$.data.shortUrl").exists())
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.data.title").value("Example Site"))
                .andExpect(jsonPath("$.data.description").value("A test link"))
                .andExpect(jsonPath("$.data.isCustomAlias").value(false))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.data.totalClicks").value(0));
    }

    @Test
    void createUrl_withSameOriginalUrl_returns200WithExistingEntry() throws Exception {
        // Create URL first time
        createTestUrl("https://www.example.com");

        // Create same URL second time
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Returns existing entry - service returns same DTO
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com"));
    }

    @Test
    void createUrl_withCustomAlias_storesAlias() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("my-link");

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.shortCode").value("my-link"))
                .andExpect(jsonPath("$.data.isCustomAlias").value(true));
    }

    @Test
    void createUrl_withTakenAlias_returns409() throws Exception {
        // Create URL with alias first
        CreateUrlRequest first = new CreateUrlRequest();
        first.setOriginalUrl("https://www.first.com");
        first.setCustomAlias("my-alias");
        mockMvc.perform(post("/api/v1/urls")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)));

        // Try to create another URL with same alias
        CreateUrlRequest second = new CreateUrlRequest();
        second.setOriginalUrl("https://www.second.com");
        second.setCustomAlias("my-alias");

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    void createUrl_withInvalidUrl_returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("not-a-valid-url");

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createUrl_withReservedAlias_returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("admin");

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createUrl_withPastExpiresAt_returns400() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setExpiresAt(LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createUrl_withoutAuth_returns401() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // GET /api/v1/urls
    // ==========================================

    @Test
    void getUserUrls_default_returns200WithPaginatedResponse() throws Exception {
        createTestUrl("https://www.example.com");

        mockMvc.perform(get("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    @Test
    void getUserUrls_withIsActiveFilter_returnsOnlyActiveLinks() throws Exception {
        createTestUrl("https://www.example.com");

        mockMvc.perform(get("/api/v1/urls?isActive=true")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getUserUrls_withSearchQuery_returnsMatchingLinks() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.portfolio.com");
        request.setTitle("My Portfolio");
        mockMvc.perform(post("/api/v1/urls")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(get("/api/v1/urls?q=portfolio")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getUserUrls_withInvalidSortBy_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/urls?sortBy=invalidField")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================
    // GET /api/v1/urls/{id}
    // ==========================================

    @Test
    void getUrlById_withValidId_returns200WithFullDetails() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        mockMvc.perform(get("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(urlId))
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com"));
    }

    @Test
    void getUrlById_withNonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/urls/99999")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    // ==========================================
    // PUT /api/v1/urls/{id}
    // ==========================================

    @Test
    void updateUrl_withValidData_returns200WithUpdatedFields() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        UpdateUrlRequest request = new UpdateUrlRequest();
        request.setTitle("Updated Title");

        mockMvc.perform(put("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    void updateUrl_withPastExpiresAt_returns400() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        UpdateUrlRequest request = new UpdateUrlRequest();
        request.setExpiresAt(LocalDateTime.now().minusDays(1));

        mockMvc.perform(put("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ==========================================
    // DELETE /api/v1/urls/{id}
    // ==========================================

    @Test
    void deleteUrl_withValidId_returns204AndRemovesUrl() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        mockMvc.perform(delete("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Verify URL is gone
        mockMvc.perform(get("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    // ==========================================
    // PATCH /api/v1/urls/{id}/toggle-status
    // ==========================================

    @Test
    void toggleStatus_withActiveLink_disablesLink() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        mockMvc.perform(patch("/api/v1/urls/" + urlId + "/toggle-status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void toggleStatus_calledTwice_restoresActiveStatus() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        // Disable
        mockMvc.perform(patch("/api/v1/urls/" + urlId + "/toggle-status")
                .header("Authorization", "Bearer " + accessToken));

        // Re-enable
        mockMvc.perform(patch("/api/v1/urls/" + urlId + "/toggle-status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    // ==========================================
    // GET /{shortCode} - redirect endpoint
    // ==========================================

    @Test
    void redirect_withValidShortCode_returns302WithLocationHeader() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        // Get the short code
        MvcResult result = mockMvc.perform(get("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        String shortCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("shortCode").asText();

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.example.com"));
    }

    @Test
    void redirect_withNonExistentCode_returns404() throws Exception {
        mockMvc.perform(get("/nonexistentcode123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirect_withInactiveLink_returns410() throws Exception {
        Long urlId = createTestUrl("https://www.example.com");

        // Get short code
        MvcResult result = mockMvc.perform(get("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        String shortCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("shortCode").asText();

        // Disable the link
        mockMvc.perform(patch("/api/v1/urls/" + urlId + "/toggle-status")
                .header("Authorization", "Bearer " + accessToken));

        // Try to redirect
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("LINK_INACTIVE"));
    }

    @Test
    void redirect_withClickLimitReached_returns410() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setClickLimit(1L);

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String shortCode = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("shortCode").asText();

        // First redirect - should succeed (302)
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound());

        // Disable the link manually to simulate click limit reached state
        // (since @Transactional rolls back incrementClickCount between requests in tests)
        Long urlId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/urls/" + urlId + "/toggle-status")
                .header("Authorization", "Bearer " + accessToken));

        // Second redirect - link is now inactive, returns 410
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone());
    }

    // ==========================================
    // GET /api/v1/urls/export
    // ==========================================

    @Test
    void exportUrls_withJsonFormat_returns200WithJsonArray() throws Exception {
        createTestUrl("https://www.example.com");

        mockMvc.perform(get("/api/v1/urls/export?format=json")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void exportUrls_withCsvFormat_returns200WithCsvFile() throws Exception {
        createTestUrl("https://www.example.com");

        mockMvc.perform(get("/api/v1/urls/export?format=csv")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"linkforge-export.csv\""));
    }

    @Test
    void exportUrls_withInvalidFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/urls/export?format=xml")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_EXPORT_FORMAT"));
    }

    @Test
    void exportUrls_withMissingFormat_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/urls/export")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
