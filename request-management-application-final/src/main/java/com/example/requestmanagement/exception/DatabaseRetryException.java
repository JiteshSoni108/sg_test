package com.example.requestmanagement.exception;

public class DatabaseRetryException extends RuntimeException {

    public DatabaseRetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
