package com.linkforge.urlshortener.controller;

import com.linkforge.urlshortener.dto.request.CreateUrlRequest;
import com.linkforge.urlshortener.dto.request.UpdateUrlRequest;
import com.linkforge.urlshortener.dto.response.ApiResponse;
import com.linkforge.urlshortener.dto.response.UrlResponse;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.service.ExportService;
import com.linkforge.urlshortener.service.RedirectionService;
import com.linkforge.urlshortener.service.UrlService;
import com.linkforge.urlshortener.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

// REST controller for URL management and public redirect endpoint
@Tag(name = "URL Management", description = "Create, list, update, delete, and export short URLs")
@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final RedirectionService redirectionService;
    private final ExportService exportService;

    // POST /api/v1/urls - create a new shortened URL
    @Operation(summary = "Create short URL", description = "Shorten a URL with optional custom alias, expiry, click limit, title, and description. Returns existing entry if the same URL was already shortened.")
    @PostMapping("/api/v1/urls")
    public ResponseEntity<ApiResponse<UrlResponse>> createUrl(
            @Valid @RequestBody CreateUrlRequest request) {

        User currentUser = SecurityUtil.getCurrentUser();
        UrlResponse urlResponse = urlService.createUrl(request, currentUser.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created("Short URL created successfully", urlResponse));
    }

    // GET /api/v1/urls - list all URLs with pagination, sorting, filtering, and search
    @Operation(summary = "List all URLs", description = "Returns paginated list of all short URLs. Supports sorting, filtering by status/expiry, and full-text search.")
    @GetMapping("/api/v1/urls")
    public ResponseEntity<ApiResponse<Page<UrlResponse>>> getUserUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field: createdAt, totalClicks, expiresAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: ASC or DESC")
            @RequestParam(defaultValue = "DESC") String sortDir,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresTo,
            @Parameter(description = "Search across originalUrl, shortCode, title, description")
            @RequestParam(required = false) String q) {

        User currentUser = SecurityUtil.getCurrentUser();
        Page<UrlResponse> urls = urlService.getUserUrls(
                currentUser.getId(), page, size, sortBy, sortDir, isActive, expiresFrom, expiresTo, q);
        return ResponseEntity.ok(ApiResponse.success("URLs retrieved successfully", urls));
    }

    // GET /api/v1/urls/{id} - get single URL details by ID
    @Operation(summary = "Get URL by ID", description = "Retrieve full details of a single short URL by its database ID.")
    @GetMapping("/api/v1/urls/{id}")
    public ResponseEntity<ApiResponse<UrlResponse>> getUrlById(@PathVariable Long id) {
        User currentUser = SecurityUtil.getCurrentUser();
        UrlResponse urlResponse = urlService.getUrlById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("URL retrieved successfully", urlResponse));
    }

    // PUT /api/v1/urls/{id} - update an existing URL
    @Operation(summary = "Update URL", description = "Update any fields of an existing short URL. All fields are optional.")
    @PutMapping("/api/v1/urls/{id}")
    public ResponseEntity<ApiResponse<UrlResponse>> updateUrl(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUrlRequest request) {

        User currentUser = SecurityUtil.getCurrentUser();
        UrlResponse urlResponse = urlService.updateUrl(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Short URL updated successfully", urlResponse));
    }

    // DELETE /api/v1/urls/{id} - permanently delete a URL and its click logs
    @Operation(summary = "Delete URL", description = "Permanently delete a short URL and all its associated click logs.")
    @DeleteMapping("/api/v1/urls/{id}")
    public ResponseEntity<Void> deleteUrl(@PathVariable Long id) {
        User currentUser = SecurityUtil.getCurrentUser();
        urlService.deleteUrl(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/urls/{id}/toggle-status - toggle active/inactive status
    @Operation(summary = "Toggle URL status", description = "Toggle the active/inactive status of a short URL. Inactive links return 410 Gone on redirect.")
    @PatchMapping("/api/v1/urls/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UrlResponse>> toggleStatus(@PathVariable Long id) {
        User currentUser = SecurityUtil.getCurrentUser();
        UrlResponse urlResponse = urlService.toggleStatus(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("URL status updated successfully", urlResponse));
    }

    // GET /api/v1/urls/export - export all URLs as JSON or CSV
    @Operation(summary = "Export URLs", description = "Export all short URLs as JSON array or CSV file. Use ?format=json or ?format=csv.")
    @GetMapping("/api/v1/urls/export")
    public ResponseEntity<?> exportUrls(
            @Parameter(description = "Export format: json or csv", required = true)
            @RequestParam String format) {

        exportService.validateFormat(format);
        User currentUser = SecurityUtil.getCurrentUser();

        if ("csv".equalsIgnoreCase(format)) {
            String csvContent = exportService.exportAsCsv(currentUser.getId());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"linkforge-export.csv\"")
                    .body(csvContent);
        }

        List<UrlResponse> urls = exportService.exportAsJson(currentUser.getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(urls);
    }

    // GET /{shortCode} - public redirect endpoint at root level - no auth required
    @Operation(summary = "Redirect", description = "Resolve a short code and redirect to the original URL. Returns 302 on success, 404 if not found, 410 if inactive/expired/limit reached.")
    @SecurityRequirements
    @GetMapping("/{shortCode}")
    public void redirect(
            @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String originalUrl = redirectionService.redirect(shortCode, request);
        response.sendRedirect(originalUrl);
    }
}
