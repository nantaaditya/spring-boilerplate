package com.nantaaditya.example.properties.embedded;

public record AsyncConfiguration(
    int corePoolSize,
    int maxPoolSize,
    int queueCapacity,
    int keepAliveSeconds,
    String threadNamePrefix
) {

}
