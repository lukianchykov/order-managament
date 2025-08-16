package com.lukianchykov.ordermanagementapplication.controller.exception;

public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}