package com.nantaaditya.example.model.response;

public record MockClientResponse(
    long id,
    long userId,
    String title,
    boolean completed
) {

}
