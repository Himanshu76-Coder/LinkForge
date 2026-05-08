package com.linkforge.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkforge.urlshortener.dto.request.CreateUrlRequest;
import com.linkforge.urlshortener.dto.request.LoginRequest;
import com.linkforge.urlshortener.dto.request.RegisterRequest;
import com.linkforge.urlshortener.dto.request.UpdateUrlRequest;
import com.linkforge.urlshortener.repository.UrlRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private UrlRepository urlRepository;

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
        // Create URL first time - should be 201
        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildCreateRequest("https://www.example.com"))))
                .andExpect(status().isCreated());

        // Create same URL second time - should be 200 with existing entry
        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildCreateRequest("https://www.example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
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
        CreateUrlRequest first = new CreateUrlRequest();
        first.setOriginalUrl("https://www.first.com");
        first.setCustomAlias("my-alias");
        mockMvc.perform(post("/api/v1/urls")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)));

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
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").exists());
    }

    @Test
    void getUserUrls_withIsActiveFilter_returnsOnlyActiveLinks() throws Exception {
        createTestUrl("https://www.example.com");

        mockMvc.perform(get("/api/v1/urls?isActive=true")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
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

    @Test
    void getUserUrls_withOnlyExpiresFrom_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/urls?expiresFrom=2024-01-01T00:00:00")
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
    void updateUrl_withClearExpiresAt_removesExpiration() throws Exception {
        // Create URL with expiration
        CreateUrlRequest createRequest = new CreateUrlRequest();
        createRequest.setOriginalUrl("https://www.example.com");
        createRequest.setExpiresAt(LocalDateTime.now().plusDays(7));

        MvcResult createResult = mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        Long urlId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Clear the expiration
        UpdateUrlRequest updateRequest = new UpdateUrlRequest();
        updateRequest.setClearExpiresAt(true);

        mockMvc.perform(put("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());
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

        mockMvc.perform(patch("/api/v1/urls/" + urlId + "/toggle-status")
                .header("Authorization", "Bearer " + accessToken));

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

        MvcResult result = mockMvc.perform(get("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        String shortCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("shortCode").asText();

        mockMvc.perform(patch("/api/v1/urls/" + urlId + "/toggle-status")
                .header("Authorization", "Bearer " + accessToken));

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("LINK_INACTIVE"));
    }

    @Test
    void redirect_withExpiredLink_returns410() throws Exception {
        // Create a URL with an expiry in the past by creating it normally then
        // directly setting expiresAt to the past via the repository
        Long urlId = createTestUrl("https://www.example.com");

        MvcResult result = mockMvc.perform(get("/api/v1/urls/" + urlId)
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();
        String shortCode = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("shortCode").asText();

        // Backdate the expiry directly in the DB to simulate an expired link
        urlRepository.findById(urlId).ifPresent(url -> {
            url.setExpiresAt(LocalDateTime.now().minusHours(1));
            urlRepository.save(url);
        });

        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("LINK_EXPIRED"));
    }

    @Test
    void redirect_withClickLimitReached_returns410() throws Exception {
        // Create URL with click limit of 1
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
        Long urlId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // First redirect - should succeed (302)
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound());

        // Manually set totalClicks = clickLimit in the DB to simulate the limit being reached,
        // since @Transactional rolls back the increment between MockMvc requests in tests.
        urlRepository.findById(urlId).ifPresent(url -> {
            url.setTotalClicks(url.getClickLimit());
            urlRepository.save(url);
        });

        // Second redirect - click limit reached, should return 410
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("CLICK_LIMIT_EXCEEDED"));
    }

    // ==========================================
    // GET /api/v1/urls/export
    // ==========================================

    @Test
    void exportUrls_withJsonFormat_returns200WithJsonArrayContainingCreatedUrl() throws Exception {
        createTestUrl("https://www.example.com");

        MvcResult result = mockMvc.perform(get("/api/v1/urls/export?format=json")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        // Response body must be a JSON array with exactly one entry
        String body = result.getResponse().getContentAsString();
        assertThat(objectMapper.readTree(body).isArray()).isTrue();
        assertThat(objectMapper.readTree(body).size()).isEqualTo(1);
        assertThat(objectMapper.readTree(body).get(0).path("originalUrl").asText())
                .isEqualTo("https://www.example.com");
    }

    @Test
    void exportUrls_withCsvFormat_returns200WithCsvContainingCreatedUrl() throws Exception {
        createTestUrl("https://www.example.com");

        MvcResult result = mockMvc.perform(get("/api/v1/urls/export?format=csv")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"linkforge-export.csv\""))
                .andReturn();

        // Response body must contain the CSV header and the created URL
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("shortCode,shortUrl,originalUrl");
        assertThat(body).contains("https://www.example.com");
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

    // Helper to build a simple CreateUrlRequest
    private CreateUrlRequest buildCreateRequest(String url) {
        CreateUrlRequest r = new CreateUrlRequest();
        r.setOriginalUrl(url);
        return r;
    }
}
