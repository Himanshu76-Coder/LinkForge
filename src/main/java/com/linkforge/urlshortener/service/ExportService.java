package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.response.UrlResponse;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.exception.InvalidExportFormatException;
import com.linkforge.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

// Service handling URL data export in JSON and CSV formats per PRD Section 5.18
@Service
@RequiredArgsConstructor
public class ExportService {

    private final UrlRepository urlRepository;
    private final UrlService urlService;

    // Supported export formats
    private static final String FORMAT_JSON = "json";
    private static final String FORMAT_CSV  = "csv";

    // CSV column headers matching all URL fields - PRD BR-64
    private static final String CSV_HEADER =
            "id,shortCode,shortUrl,originalUrl,title,description,isCustomAlias,isActive,totalClicks,clickLimit,expiresAt,createdAt,updatedAt";

    // Get all user URLs as a list of UrlResponse DTOs (used for JSON export)
    public List<UrlResponse> exportAsJson(Long userId) {
        List<Url> urls = urlRepository.findByUserId(
                userId, Sort.by(Sort.Direction.DESC, "createdAt"));
        return urls.stream()
                .map(urlService::mapToUrlResponse)
                .toList();
    }

    // Build CSV string from all user URLs (used for CSV export)
    public String exportAsCsv(Long userId) {
        List<Url> urls = urlRepository.findByUserId(
                userId, Sort.by(Sort.Direction.DESC, "createdAt"));

        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append("\n");

        for (Url url : urls) {
            UrlResponse r = urlService.mapToUrlResponse(url);
            csv.append(escapeCsv(String.valueOf(r.getId()))).append(",")
               .append(escapeCsv(r.getShortCode())).append(",")
               .append(escapeCsv(r.getShortUrl())).append(",")
               .append(escapeCsv(r.getOriginalUrl())).append(",")
               .append(escapeCsv(r.getTitle())).append(",")
               .append(escapeCsv(r.getDescription())).append(",")
               .append(r.getIsCustomAlias()).append(",")
               .append(r.getIsActive()).append(",")
               .append(r.getTotalClicks()).append(",")
               .append(r.getClickLimit() != null ? r.getClickLimit() : "").append(",")
               .append(r.getExpiresAt() != null ? r.getExpiresAt() : "").append(",")
               .append(r.getCreatedAt() != null ? r.getCreatedAt() : "").append(",")
               .append(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
               .append("\n");
        }

        return csv.toString();
    }

    // Validate the format parameter - PRD BR-63
    public void validateFormat(String format) {
        if (format == null || format.isBlank()) {
            throw new InvalidExportFormatException("Export format is required. Use 'json' or 'csv'");
        }
        if (!FORMAT_JSON.equalsIgnoreCase(format) && !FORMAT_CSV.equalsIgnoreCase(format)) {
            throw new InvalidExportFormatException(
                    "Invalid export format '" + format + "'. Supported formats: json, csv");
        }
    }

    // Wrap a CSV field value in quotes if it contains commas, quotes, or newlines
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
