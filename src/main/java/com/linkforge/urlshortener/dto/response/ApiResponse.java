package com.linkforge.urlshortener.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Generic wrapper for all API success responses
@Getter
@Setter
public class ApiResponse<T> {

    private Integer status;
    private Boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse(Integer status, Boolean success, String message, T data) {
        this.status = status;
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // 200 OK with custom message and data
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, true, message, data);
    }

    // 201 Created with custom message and data
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, true, message, data);
    }
}
