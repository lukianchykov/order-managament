package com.lukianchykov.ordermanagementapplication.controller.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}