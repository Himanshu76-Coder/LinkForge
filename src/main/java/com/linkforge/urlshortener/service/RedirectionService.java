package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.entity.ClickLog;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.exception.ClickLimitExceededException;
import com.linkforge.urlshortener.exception.LinkNotActiveException;
import com.linkforge.urlshortener.exception.UrlExpiredException;
import com.linkforge.urlshortener.repository.ClickLogRepository;
import com.linkforge.urlshortener.repository.UrlRepository;
import com.linkforge.urlshortener.util.RequestContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Service handling URL redirection logic and click tracking
@Service
@RequiredArgsConstructor
public class RedirectionService {

    private final UrlService urlService;
    private final UrlRepository urlRepository;
    private final ClickLogRepository clickLogRepository;

    // Resolve short code, validate all redirect conditions, log click, return original URL
    // Checks are evaluated in order per PRD BR-13: existence → active → expiry → click limit
    @Transactional
    public String redirect(String shortCode, HttpServletRequest request) {
        // BR-08: Find URL by short code - throws 404 if not found
        Url url = urlService.getUrlEntityByShortCode(shortCode);

        // BR-09: Check if URL is active - PRD BR-09
        if (!url.getIsActive()) {
            throw new LinkNotActiveException("This link is inactive");
        }

        // BR-10: Check if URL has expired - PRD BR-10
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("This link has expired");
        }

        // BR-11: Check if click limit has been reached - PRD BR-11
        if (url.getClickLimit() != null && url.getTotalClicks() >= url.getClickLimit()) {
            throw new ClickLimitExceededException("This link has reached its maximum click limit");
        }

        // BR-12: Increment click count atomically - PRD BR-12
        urlRepository.incrementClickCount(url.getId());

        // BR-12: Log the click event - PRD BR-12
        logClick(url, request);

        return url.getOriginalUrl();
    }

    // Save a click log entry with visitor metadata
    private void logClick(Url url, HttpServletRequest request) {
        ClickLog clickLog = new ClickLog();
        clickLog.setUrl(url);
        clickLog.setIpAddress(RequestContextUtil.getClientIp(request));
        clickLog.setUserAgent(RequestContextUtil.getUserAgent(request));
        clickLog.setReferrer(RequestContextUtil.getReferrer(request));
        // Country is null by default - requires GeoIP integration (future enhancement)
        clickLogRepository.save(clickLog);
    }
}
