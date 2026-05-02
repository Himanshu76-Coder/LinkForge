package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.request.CreateUrlRequest;
import com.linkforge.urlshortener.dto.request.UpdateUrlRequest;
import com.linkforge.urlshortener.dto.response.UrlResponse;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.entity.User;
import com.linkforge.urlshortener.exception.*;
import com.linkforge.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// Unit tests for UrlService - repositories and dependencies are mocked
@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UrlService urlService;

    private User testUser;
    private Url testUrl;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john@example.com");

        testUrl = new Url();
        testUrl.setId(1L);
        testUrl.setUser(testUser);
        testUrl.setShortCode("abc123");
        testUrl.setOriginalUrl("https://www.example.com");
        testUrl.setIsCustomAlias(false);
        testUrl.setIsActive(true);
        testUrl.setTotalClicks(0L);
    }

    // ==========================================
    // createUrl() tests
    // ==========================================

    @Test
    void createUrl_withValidData_returnsUrlResponse() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        when(userService.findUserById(1L)).thenReturn(testUser);
        when(urlRepository.findByUserIdAndOriginalUrl(1L, "https://www.example.com"))
                .thenReturn(Optional.empty());
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

        UrlResponse response = urlService.createUrl(request, 1L);

        assertThat(response.getOriginalUrl()).isEqualTo("https://www.example.com");
        assertThat(response.getShortUrl()).startsWith("http://localhost:8080/");
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void createUrl_withCustomAlias_storesAlias() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("my-link");

        testUrl.setShortCode("my-link");
        testUrl.setIsCustomAlias(true);

        when(userService.findUserById(1L)).thenReturn(testUser);
        when(urlRepository.findByUserIdAndOriginalUrl(1L, "https://www.example.com"))
                .thenReturn(Optional.empty());
        when(urlRepository.existsByShortCode("my-link")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

        UrlResponse response = urlService.createUrl(request, 1L);

        assertThat(response.getShortCode()).isEqualTo("my-link");
        assertThat(response.getIsCustomAlias()).isTrue();
    }

    @Test
    void createUrl_withDuplicateOriginalUrl_returnsExistingEntry() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");

        when(userService.findUserById(1L)).thenReturn(testUser);
        when(urlRepository.findByUserIdAndOriginalUrl(1L, "https://www.example.com"))
                .thenReturn(Optional.of(testUrl));

        UrlResponse response = urlService.createUrl(request, 1L);

        // Returns existing entry without saving a new one
        assertThat(response.getShortCode()).isEqualTo("abc123");
        verify(urlRepository, never()).save(any());
    }

    @Test
    void createUrl_withTakenAlias_throwsDuplicateResourceException() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("taken-alias");

        when(userService.findUserById(1L)).thenReturn(testUser);
        when(urlRepository.findByUserIdAndOriginalUrl(1L, "https://www.example.com"))
                .thenReturn(Optional.empty());
        when(urlRepository.existsByShortCode("taken-alias")).thenReturn(true);

        assertThatThrownBy(() -> urlService.createUrl(request, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void createUrl_withReservedAlias_throwsInvalidAliasException() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setCustomAlias("admin");

        when(userService.findUserById(1L)).thenReturn(testUser);
        when(urlRepository.findByUserIdAndOriginalUrl(1L, "https://www.example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.createUrl(request, 1L))
                .isInstanceOf(InvalidAliasException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    void createUrl_withPastExpiresAt_throwsInvalidRequestException() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://www.example.com");
        request.setExpiresAt(LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> urlService.createUrl(request, 1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("future");
    }

    @Test
    void createUrl_withInvalidUrl_throwsInvalidRequestException() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("not-a-valid-url");

        assertThatThrownBy(() -> urlService.createUrl(request, 1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid URL format");
    }

    @Test
    void createUrl_withFtpUrl_throwsInvalidRequestException() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("ftp://example.com");

        assertThatThrownBy(() -> urlService.createUrl(request, 1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Invalid URL format");
    }

    // ==========================================
    // getUrlById() tests
    // ==========================================

    @Test
    void getUrlById_withValidId_returnsUrlResponse() {
        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));

        UrlResponse response = urlService.getUrlById(1L, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getShortCode()).isEqualTo("abc123");
    }

    @Test
    void getUrlById_withNonExistentId_throwsResourceNotFoundException() {
        when(urlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.getUrlById(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("URL not found");
    }

    @Test
    void getUrlById_withDifferentOwner_throwsResourceNotFoundException() {
        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));

        // User ID 2 does not own this URL (owned by user 1)
        assertThatThrownBy(() -> urlService.getUrlById(1L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==========================================
    // updateUrl() tests
    // ==========================================

    @Test
    void updateUrl_withValidData_returnsUpdatedResponse() {
        UpdateUrlRequest request = new UpdateUrlRequest();
        request.setTitle("New Title");

        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));
        when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

        UrlResponse response = urlService.updateUrl(1L, request, 1L);

        assertThat(response).isNotNull();
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    void updateUrl_withTakenAlias_throwsDuplicateResourceException() {
        UpdateUrlRequest request = new UpdateUrlRequest();
        request.setCustomAlias("taken-alias");

        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));
        when(urlRepository.existsByShortCode("taken-alias")).thenReturn(true);

        assertThatThrownBy(() -> urlService.updateUrl(1L, request, 1L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void updateUrl_withPastExpiresAt_throwsInvalidRequestException() {
        UpdateUrlRequest request = new UpdateUrlRequest();
        request.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));

        assertThatThrownBy(() -> urlService.updateUrl(1L, request, 1L))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("future");
    }

    // ==========================================
    // deleteUrl() tests
    // ==========================================

    @Test
    void deleteUrl_withValidId_deletesUrlAndLogs() {
        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));

        urlService.deleteUrl(1L, 1L);

        verify(urlRepository).delete(testUrl);
    }

    // ==========================================
    // toggleStatus() tests
    // ==========================================

    @Test
    void toggleStatus_withActiveLink_disablesLink() {
        testUrl.setIsActive(true);
        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        UrlResponse response = urlService.toggleStatus(1L, 1L);

        assertThat(response.getIsActive()).isFalse();
    }

    @Test
    void toggleStatus_withInactiveLink_enablesLink() {
        testUrl.setIsActive(false);
        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));
        when(urlRepository.save(any(Url.class))).thenAnswer(inv -> inv.getArgument(0));

        UrlResponse response = urlService.toggleStatus(1L, 1L);

        assertThat(response.getIsActive()).isTrue();
    }
}
