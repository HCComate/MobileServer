package com.semse.mobile_server.config;

public class AdminPcException extends RuntimeException {

    private final int statusCode;

    public AdminPcException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
