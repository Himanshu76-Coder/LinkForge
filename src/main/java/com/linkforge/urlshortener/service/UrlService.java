package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.CreateUrlRequest;
import com.linkforge.urlshortener.dto.request.UpdateUrlRequest;
import com.linkforge.urlshortener.dto.response.UrlResponse;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.input.InvalidAliasException;
import com.linkforge.urlshortener.exception.input.InvalidRequestException;
import com.linkforge.urlshortener.exception.resource.DuplicateResourceException;
import com.linkforge.urlshortener.exception.resource.ResourceNotFoundException;
import com.linkforge.urlshortener.exception.url.ShortCodeGenerationException;
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

    // Creates a new shortened URL. Returns the existing entry (created=false) if this user
    // already shortened the same original URL without a custom alias.
    @Transactional
    public UrlServiceResult createUrl(CreateUrlRequest request, Long userId) {
        // Validate original URL is provided
        if (request.getOriginalUrl() == null || request.getOriginalUrl().isBlank()) {
            throw new InvalidRequestException("Original URL is required");
        }

        // Validate URL format and protocol
        if (!UrlValidator.isValid(request.getOriginalUrl())) {
            throw new InvalidRequestException("Invalid URL format. Only http and https URLs are allowed");
        }

        // Block URLs pointing to localhost or private IP ranges
        if (!UrlValidator.isSafe(request.getOriginalUrl())) {
            throw new InvalidRequestException("URL points to a private or reserved address and cannot be shortened");
        }

        // Expiration date must be in the future if provided
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new InvalidRequestException("Expiration date must be in the future");
        }

        User user = userService.findUserById(userId);

        // If this original URL already exists for the user, return it or throw depending on alias
        Optional<Url> existing = urlRepository.findByUserIdAndOriginalUrl(userId, request.getOriginalUrl());
        if (existing.isPresent()) {
            if (request.getCustomAlias() == null || request.getCustomAlias().isBlank()) {
                // No alias requested — return the existing entry
                return new UrlServiceResult(mapToUrlResponse(existing.get()), false);
            }
            // Custom alias requested but this original URL is already shortened by this user
            throw new InvalidRequestException(
                    "You already have this URL shortened as '" + existing.get().getShortCode() +
                    "'. Update the existing entry or use a different original URL.");
        }

        // Resolve short code
        String shortCode;
        boolean isCustomAlias;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            // Validate and use the provided custom alias
            shortCode = validateAndResolveAlias(request.getCustomAlias());
            isCustomAlias = true;
        } else {
            // Auto-generate a unique short code
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
        return new UrlServiceResult(mapToUrlResponse(saved), true);
    }

    // Get a single URL by ID - PRD Section 5.6
    public UrlResponse getUrlById(Long urlId, Long userId) {
        Url url = findUrlByIdAndUser(urlId, userId);
        return mapToUrlResponse(url);
    }

    // Returns a paginated list of URLs with optional search, sorting, and filters.
    public Page<UrlResponse> getUserUrls(Long userId,
                                          int page,
                                          int size,
                                          String sortBy,
                                          String sortDir,
                                          Boolean isActive,
                                          LocalDateTime expiresFrom,
                                          LocalDateTime expiresTo,
                                          String q) {
        // Reject unknown sort fields to prevent JPA errors
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new InvalidRequestException("Invalid sortBy value. Allowed values: createdAt, totalClicks, expiresAt");
        }

        // Both date range filters must be provided together or not at all
        if ((expiresFrom != null && expiresTo == null) || (expiresFrom == null && expiresTo != null)) {
            throw new InvalidRequestException("Both expiresFrom and expiresTo must be provided together");
        }

        // Combining search with other filters would produce confusing results — reject early
        if (q != null && !q.isBlank() && (isActive != null || expiresFrom != null)) {
            throw new InvalidRequestException(
                    "Search (q) cannot be combined with isActive or expiresFrom/expiresTo filters. Use one at a time.");
        }

        // Reject combining isActive and expiresFrom together
        if (isActive != null && expiresFrom != null) {
            throw new InvalidRequestException(
                    "isActive and expiresFrom/expiresTo filters cannot be combined. Use one at a time.");
        }

        // Clamp page size to the configured maximum
        int clampedSize = Math.min(size, MAX_PAGE_SIZE);
        if (clampedSize <= 0) clampedSize = DEFAULT_PAGE_SIZE;

        // Spring Data throws IllegalArgumentException for page < 0, so reject it early
        if (page < 0) {
            throw new InvalidRequestException("Page number must not be negative");
        }

        // Build sort direction
        Sort sort = "ASC".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, clampedSize, sort);

        // Apply search query if provided
        if (q != null && !q.isBlank()) {
            return urlRepository.searchByUserId(userId, q.trim(), pageable)
                    .map(this::mapToUrlResponse);
        }

        // Apply isActive filter if provided
        if (isActive != null) {
            return urlRepository.findByUserIdAndIsActive(userId, isActive, pageable)
                    .map(this::mapToUrlResponse);
        }

        // Apply expiry range filter if provided
        if (expiresFrom != null) {
            return urlRepository.findByUserIdAndExpiresAtBetween(userId, expiresFrom, expiresTo, pageable)
                    .map(this::mapToUrlResponse);
        }

        // Default: return all user URLs paginated
        return urlRepository.findByUserId(userId, pageable).map(this::mapToUrlResponse);
    }

    // Updates an existing URL. Only fields present in the request are changed.
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
            // Check that the new originalUrl is not already shortened by this user under a different entry
            if (!request.getOriginalUrl().equals(url.getOriginalUrl())) {
                urlRepository.findByUserIdAndOriginalUrl(userId, request.getOriginalUrl())
                        .ifPresent(existing -> {
                            throw new InvalidRequestException(
                                    "You already have this URL shortened as '" + existing.getShortCode() +
                                    "'. Update the existing entry or use a different original URL.");
                        });
            }
            url.setOriginalUrl(request.getOriginalUrl());
        }

        // Update custom alias if provided
        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            // Skip validation if the alias is unchanged
            if (!request.getCustomAlias().equals(url.getShortCode())) {
                String newAlias = validateAndResolveAlias(request.getCustomAlias());
                url.setShortCode(newAlias);
                url.setIsCustomAlias(true);
            }
        }

        // Update title if provided (empty string clears the field)
        if (request.getTitle() != null) {
            url.setTitle(request.getTitle().isBlank() ? null : request.getTitle());
        }

        // Update description if provided (empty string clears the field)
        if (request.getDescription() != null) {
            url.setDescription(request.getDescription().isBlank() ? null : request.getDescription());
        }

        // Reject conflicting expiry instructions — only one can be used at a time
        if (request.isClearExpiresAt() && request.getExpiresAt() != null) {
            throw new InvalidRequestException(
                    "clearExpiresAt and expiresAt cannot be provided together. Use one or the other.");
        }

        // Clear or update the expiration date
        if (request.isClearExpiresAt()) {
            url.setExpiresAt(null);
        } else if (request.getExpiresAt() != null) {
            if (!request.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new InvalidRequestException("Expiration date must be in the future");
            }
            url.setExpiresAt(request.getExpiresAt());
        }

        // Reject conflicting click limit instructions — only one can be used at a time
        if (request.isClearClickLimit() && request.getClickLimit() != null) {
            throw new InvalidRequestException(
                    "clearClickLimit and clickLimit cannot be provided together. Use one or the other.");
        }

        // Clear or update the click limit
        if (request.isClearClickLimit()) {
            url.setClickLimit(null);
        } else if (request.getClickLimit() != null) {
            url.setClickLimit(request.getClickLimit());
        }

        Url updated = urlRepository.save(url);
        return mapToUrlResponse(updated);
    }

    // Permanently deletes a URL and all its associated click logs.
    @Transactional
    public void deleteUrl(Long urlId, Long userId) {
        Url url = findUrlByIdAndUser(urlId, userId);
        // Cascade deletion removes all associated click logs
        urlRepository.delete(url);
    }

    // Flips the active/inactive status of a URL.
    @Transactional
    public UrlResponse toggleStatus(Long urlId, Long userId) {
        Url url = findUrlByIdAndUser(urlId, userId);
        // Flip the current status
        url.setIsActive(!url.getIsActive());
        Url updated = urlRepository.save(url);
        return mapToUrlResponse(updated);
    }

    // Get URL entity by short code for redirection (internal use)
    public Url getUrlEntityByShortCode(String shortCode) {
        return urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found"));
    }

    // Validates a custom alias against reserved words and checks it is not already taken.
    private String validateAndResolveAlias(String alias) {
        // Use Locale.ROOT for consistent case-insensitive comparison
        if (RESERVED_WORDS.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new InvalidAliasException("Alias '" + alias + "' is reserved and cannot be used");
        }

        // Check uniqueness
        if (urlRepository.existsByShortCode(alias)) {
            throw new DuplicateResourceException("Alias '" + alias + "' is already taken");
        }

        return alias;
    }

    // Generates a unique short code, retrying up to MAX_RETRIES times on collision.
    private String generateUniqueShortCode() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            String code = ShortCodeGenerator.generate();
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new ShortCodeGenerationException("Failed to generate a unique short code after " + MAX_RETRIES + " attempts");
    }

    // Finds a URL by ID and verifies ownership in a single query.
    private Url findUrlByIdAndUser(Long urlId, Long userId) {
        return urlRepository.findByIdAndUserId(urlId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found with id: " + urlId));
    }

    // Maps a Url entity to a UrlResponse DTO, building the full short URL from the base URL.
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

    // Simple result wrapper so the controller knows whether a URL was newly created or already existed
    public record UrlServiceResult(UrlResponse urlResponse, boolean created) {}
}
