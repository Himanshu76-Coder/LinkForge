package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.CreateUrlRequest;
import com.linkforge.urlshortener.dto.request.UpdateUrlRequest;
import com.linkforge.urlshortener.dto.response.UrlResponse;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.*;
import com.linkforge.urlshortener.repository.UrlRepository;
import com.linkforge.urlshortener.util.ShortCodeGenerator;
import com.linkforge.urlshortener.util.UrlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

// Service handling all URL management business logic
@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserService userService;

    @Value("${app.base-url}")
    private String baseUrl;

    // Reserved words that cannot be used as custom aliases - PRD BR-17
    private static final List<String> RESERVED_WORDS =
            List.of("api", "admin", "health", "login", "register", "swagger", "v1", "static");

    // Allowed sort fields - PRD BR-50
    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("createdAt", "totalClicks", "expiresAt");

    // Default and max page sizes - PRD BR-49
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    // Short code generation max retries - PRD Section 11.5
    private static final int MAX_RETRIES = 5;

    // Create a new shortened URL - PRD Section 5.1
    @Transactional
    public UrlResponse createUrl(CreateUrlRequest request, Long userId) {
        // Validate original URL is provided
        if (request.getOriginalUrl() == null || request.getOriginalUrl().isBlank()) {
            throw new InvalidRequestException("Original URL is required");
        }

        // Validate URL format and protocol - PRD BR-41, BR-42
        if (!UrlValidator.isValid(request.getOriginalUrl())) {
            throw new InvalidRequestException("Invalid URL format. Only http and https URLs are allowed");
        }

        // Block URLs pointing to localhost or private IP ranges
        if (!UrlValidator.isSafe(request.getOriginalUrl())) {
            throw new InvalidRequestException("URL points to a private or reserved address and cannot be shortened");
        }

        // Validate expiration is in the future if provided - PRD BR-20
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Expiration date must be in the future");
        }

        User user = userService.findUserById(userId);

        // Check for duplicate original URL for this user - PRD BR-44, BR-45
        Optional<Url> existing = urlRepository.findByUserIdAndOriginalUrl(userId, request.getOriginalUrl());
        if (existing.isPresent()) {
            // Return existing entry instead of creating a duplicate - PRD BR-45
            return mapToUrlResponse(existing.get());
        }

        // Resolve short code
        String shortCode;
        boolean isCustomAlias;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            // Validate and use custom alias - PRD BR-15, BR-16, BR-17
            shortCode = validateAndResolveAlias(request.getCustomAlias());
            isCustomAlias = true;
        } else {
            // Auto-generate unique short code - PRD BR-01, BR-04
            shortCode = generateUniqueShortCode();
            isCustomAlias = false;
        }

        // Build and save URL entity
        Url url = new Url();
        url.setUser(user);
        url.setShortCode(shortCode);
        url.setOriginalUrl(request.getOriginalUrl());
        url.setTitle(request.getTitle());
        url.setDescription(request.getDescription());
        url.setIsCustomAlias(isCustomAlias);
        url.setIsActive(true);
        url.setTotalClicks(0L);
        url.setClickLimit(request.getClickLimit());
        url.setExpiresAt(request.getExpiresAt());

        Url saved = urlRepository.save(url);
        return mapToUrlResponse(saved);
    }

    // Get a single URL by ID - PRD Section 5.6
    public UrlResponse getUrlById(Long urlId, Long userId) {
        Url url = findUrlByIdAndUser(urlId, userId);
        return mapToUrlResponse(url);
    }

    // Get paginated list of URLs with optional search and filters - PRD Section 5.5, 5.14, 5.15
    public Page<UrlResponse> getUserUrls(Long userId,
                                          int page,
                                          int size,
                                          String sortBy,
                                          String sortDir,
                                          Boolean isActive,
                                          LocalDateTime expiresFrom,
                                          LocalDateTime expiresTo,
                                          String q) {
        // Validate sortBy field - PRD BR-50
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("Invalid sortBy value. Allowed values: createdAt, totalClicks, expiresAt");
        }

        // Clamp page size to max - PRD BR-49
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        if (clampedSize <= 0) clampedSize = DEFAULT_PAGE_SIZE;

        // Build sort direction
        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, clampedSize, sort);

        // Apply search if query provided - PRD BR-53
        if (q != null && !q.isBlank()) {
            return urlRepository.searchByUserId(userId, q.trim(), pageable)
                    .map(this::mapToUrlResponse);
        }

        // Apply isActive filter if provided - PRD BR-51
        if (isActive != null) {
            return urlRepository.findByUserIdAndIsActive(userId, isActive, pageable)
                    .map(this::mapToUrlResponse);
        }

        // Apply expiry range filter if provided - PRD BR-51
        if (expiresFrom != null && expiresTo != null) {
            return urlRepository.findByUserIdAndExpiresAtBetween(userId, expiresFrom, expiresTo, pageable)
                    .map(this::mapToUrlResponse);
        }

        // Default: return all user URLs paginated
        return urlRepository.findByUserId(userId, pageable).map(this::mapToUrlResponse);
    }

    // Update an existing URL - PRD Section 5.7
    @Transactional
    public UrlResponse updateUrl(Long urlId, UpdateUrlRequest request, Long userId) {
        Url url = findUrlByIdAndUser(urlId, userId);

        // Update original URL if provided
        if (request.getOriginalUrl() != null && !request.getOriginalUrl().isBlank()) {
            if (!UrlValidator.isValid(request.getOriginalUrl())) {
                throw new InvalidRequestException("Invalid URL format. Only http and https URLs are allowed");
            }
            if (!UrlValidator.isSafe(request.getOriginalUrl())) {
                throw new InvalidRequestException("URL points to a private or reserved address and cannot be shortened");
            }
            url.setOriginalUrl(request.getOriginalUrl());
        }

        // Update custom alias if provided - PRD BR-31
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            // Only validate if alias is actually changing
            if (!request.getCustomAlias().equals(url.getShortCode())) {
                String newAlias = validateAndResolveAlias(request.getCustomAlias());
                url.setShortCode(newAlias);
                url.setIsCustomAlias(true);
            }
        }

        // Update title if provided
        if (request.getTitle() != null) {
            url.setTitle(request.getTitle().isBlank() ? null : request.getTitle());
        }

        // Update description if provided
        if (request.getDescription() != null) {
            url.setDescription(request.getDescription().isBlank() ? null : request.getDescription());
        }

        // Update expiration if provided - PRD BR-32
        if (request.getExpiresAt() != null) {
            if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new InvalidRequestException("Expiration date must be in the future");
            }
            url.setExpiresAt(request.getExpiresAt());
        }

        // Update click limit if provided
        if (request.getClickLimit() != null) {
            url.setClickLimit(request.getClickLimit());
        }

        Url updated = urlRepository.save(url);
        return mapToUrlResponse(updated);
    }

    // Delete a URL permanently - PRD Section 5.8
    @Transactional
    public void deleteUrl(Long urlId, Long userId) {
        Url url = findUrlByIdAndUser(urlId, userId);
        // Cascade deletion removes all associated click_logs - PRD BR-34
        urlRepository.delete(url);
    }

    // Toggle active/inactive status - PRD Section 5.9
    @Transactional
    public UrlResponse toggleStatus(Long urlId, Long userId) {
        Url url = findUrlByIdAndUser(urlId, userId);
        // Flip the current status - PRD BR-36
        url.setIsActive(!url.getIsActive());
        Url updated = urlRepository.save(url);
        return mapToUrlResponse(updated);
    }

    // Get URL entity by short code for redirection (internal use)
    public Url getUrlEntityByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
    }

    // Validate custom alias format, reserved words, and uniqueness
    private String validateAndResolveAlias(String alias) {
        // Check reserved words using Locale.ROOT for consistent comparison - PRD BR-17
        if (RESERVED_WORDS.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new InvalidAliasException("Alias '" + alias + "' is reserved and cannot be used");
        }

        // Check uniqueness - PRD BR-16
        if (urlRepository.existsByShortCode(alias)) {
            throw new DuplicateResourceException("Alias '" + alias + "' is already taken");
        }

        return alias;
    }

    // Generate a unique short code with retry logic - PRD BR-04, Section 11.5
    private String generateUniqueShortCode() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String code = ShortCodeGenerator.generate();
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new ShortCodeGenerationException("Failed to generate a unique short code after " + MAX_RETRIES + " attempts");
    }

    // Find URL by ID and verify ownership - throws if not found or not owned by user
    private Url findUrlByIdAndUser(Long urlId, Long userId) {
        Url url = urlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));

        // Ensure the URL belongs to the requesting user
        if (!url.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("URL not found with id: " + urlId);
        }

        return url;
    }

    // Map Url entity to UrlResponse DTO
    public UrlResponse mapToUrlResponse(Url url) {
        return new UrlResponse(
                url.getId(),
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getOriginalUrl(),
                url.getTitle(),
                url.getDescription(),
                url.getIsCustomAlias(),
                url.getIsActive(),
                url.getTotalClicks(),
                url.getClickLimit(),
                url.getExpiresAt(),
                url.getCreatedAt(),
                url.getUpdatedAt()
        );
    }
}
