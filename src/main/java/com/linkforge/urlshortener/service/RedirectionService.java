package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.entity.ClickLog;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.exception.url.ClickLimitExceededException;
import com.linkforge.urlshortener.exception.url.LinkNotActiveException;
import com.linkforge.urlshortener.exception.url.UrlExpiredException;
import com.linkforge.urlshortener.repository.ClickLogRepository;
import com.linkforge.urlshortener.repository.UrlRepository;
import com.linkforge.urlshortener.util.RequestContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Resolves a short code, validates all redirect conditions, logs the click, and returns the original URL.
// Checks run in order: existence → active → expiry → click limit.
@Service
@RequiredArgsConstructor
public class RedirectionService {

    private final UrlService urlService;
    private final UrlRepository urlRepository;
    private final ClickLogRepository clickLogRepository;

    @Transactional
    public String redirect(String shortCode, HttpServletRequest request) {
        // Find URL by short code — throws 404 if not found
        Url url = urlService.getUrlEntityByShortCode(shortCode);

        // Reject if the link has been deactivated by its owner
        if (!url.getIsActive()) {
            throw new LinkNotActiveException("This link is inactive");
        }

        // Reject if the link has passed its expiration date
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("This link has expired");
        }

        // Atomically increment the click count only if the limit has not been reached.
        // Returns 1 on success, 0 if the click limit was already hit
        int updated = urlRepository.incrementClickCountIfAllowed(url.getId());
        if (updated == 0) {
            throw new ClickLimitExceededException("This link has reached its maximum click limit");
        }

        // Flush so totalClicks reflects the increment if the entity is re-read in this transaction
        urlRepository.flush();

        // Log the click event
        logClick(url, request);

        return url.getOriginalUrl();
    }

    // Saves a click log entry with the visitor's IP, User-Agent, and referrer.
    private void logClick(Url url, HttpServletRequest request) {
        ClickLog clickLog = new ClickLog();
        clickLog.setUrl(url);
        clickLog.setIpAddress(RequestContextUtil.getClientIp(request));
        clickLog.setUserAgent(RequestContextUtil.getUserAgent(request));
        clickLog.setReferrer(RequestContextUtil.getReferrer(request));
        clickLogRepository.save(clickLog);
    }
}
