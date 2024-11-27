package com.nantaaditya.example.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExampleRequest(
    @NotBlank(message = "NotBlank") String name,
    @Min(value = 1, message = "BelowThreshold") int age
) {
}
