package com.ngoconnect.exception;

public class ConflictException extends BadRequestException {
    public ConflictException(String message) {
        super(message);
    }
}
