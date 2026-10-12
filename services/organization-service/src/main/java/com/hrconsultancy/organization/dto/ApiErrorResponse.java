package com.hrconsultancy.organization.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class ApiErrorResponse {
    private final OffsetDateTime timeStamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String requestId;
    private final List<String> details;

    public ApiErrorResponse(
            OffsetDateTime timeStamp,
            int status,
            String error,
            String message,
            String path,
            String requestId,
            List<String> details) {
        this.timeStamp = timeStamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
        this.details = details;
    }

    public OffsetDateTime getTimeStamp() {
        return timeStamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getRequestId() {
        return requestId;
    }

    public List<String> getDetails() {
        return details;
    }

}
