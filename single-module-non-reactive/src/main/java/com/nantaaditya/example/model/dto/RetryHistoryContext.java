package com.nantaaditya.example.model.dto;

public record RetryHistoryContext(
    int counter,
    String response,
    String lastError
) {

}
