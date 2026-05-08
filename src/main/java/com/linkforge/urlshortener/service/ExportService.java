package com.linkforge.urlshortener.service;

import com.linkforge.urlshortener.dto.response.UrlResponse;
import com.linkforge.urlshortener.entity.Url;
import com.linkforge.urlshortener.exception.input.InvalidExportFormatException;
import com.linkforge.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

// Handles URL data export in JSON and CSV formats, capped at EXPORT_ROW_CAP rows.
@Service
@RequiredArgsConstructor
public class ExportService {

    private final UrlRepository urlRepository;
    private final UrlService urlService;

    // Supported export formats
    private static final String FORMAT_JSON = "json";
    private static final String FORMAT_CSV  = "csv";

    // Maximum rows returned per export — enforced at the DB level via Pageable LIMIT.
    static final int EXPORT_ROW_CAP = 5_000;

    // CSV column headers matching all URL fields - PRD BR-64
    private static final String CSV_HEADER =
            "id,shortCode,shortUrl,originalUrl,title,description,isCustomAlias,isActive,totalClicks,clickLimit,expiresAt,createdAt,updatedAt";

    // Holds the export result and a flag indicating whether the cap was reached
    public record ExportResult<T>(T data, boolean truncated, int count) {}

    // Exports the user's URLs as a JSON list, ordered by createdAt DESC.
    public ExportResult<List<UrlResponse>> exportAsJson(Long userId) {
        List<Url> urls = urlRepository.findByUserIdForExport(
                userId, PageRequest.of(0, EXPORT_ROW_CAP));
        List<UrlResponse> data = urls.stream()
                .map(urlService::mapToUrlResponse)
                .toList();
        return new ExportResult<>(data, urls.size() == EXPORT_ROW_CAP, urls.size());
    }

    // Exports the user's URLs as a CSV string, ordered by createdAt DESC.
    public ExportResult<String> exportAsCsv(Long userId) {
        List<Url> urls = urlRepository.findByUserIdForExport(
                userId, PageRequest.of(0, EXPORT_ROW_CAP));

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

        return new ExportResult<>(csv.toString(), urls.size() == EXPORT_ROW_CAP, urls.size());
    }

    // Validates that the format is either "json" or "csv".
    public void validateFormat(String format) {
        if (format == null || format.isBlank()) {
            throw new InvalidExportFormatException("Export format is required. Use 'json' or 'csv'");
        }
        if (!FORMAT_JSON.equalsIgnoreCase(format) && !FORMAT_CSV.equalsIgnoreCase(format)) {
            throw new InvalidExportFormatException(
                    "Invalid export format '" + format + "'. Supported formats: json, csv");
        }
    }

    // Wraps a CSV field in quotes if it contains commas, quotes, or newlines.
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
