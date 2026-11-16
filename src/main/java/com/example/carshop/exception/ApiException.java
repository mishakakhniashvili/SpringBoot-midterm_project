package com.example.carshop.exception;
import org.springframework.http.HttpStatus;
import lombok.Getter;
public class ApiException extends RuntimeException {
    @Getter
    private final HttpStatus status;
    public ApiException(HttpStatus status, String message) { super(message); this.status = status; }
    public static ApiException notFound(String resource) { return new ApiException(HttpStatus.NOT_FOUND, resource + " not found"); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
    public static ApiException badRequest(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }
}
