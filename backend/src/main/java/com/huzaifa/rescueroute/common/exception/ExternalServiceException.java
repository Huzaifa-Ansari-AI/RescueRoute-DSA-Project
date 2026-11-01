package com.huzaifa.rescueroute.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an external third-party service (e.g. OpenStreetMap Overpass API, OSRM)
 * encounters a failure, timeout, rate limit, or unexpected response.
 */
public class ExternalServiceException extends RuntimeException {

    private final HttpStatus status;

    public ExternalServiceException(String message) {
        super(message);
        this.status = HttpStatus.SERVICE_UNAVAILABLE;
    }

    public ExternalServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status != null ? status : HttpStatus.SERVICE_UNAVAILABLE;
    }

    public ExternalServiceException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status != null ? status : HttpStatus.SERVICE_UNAVAILABLE;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
