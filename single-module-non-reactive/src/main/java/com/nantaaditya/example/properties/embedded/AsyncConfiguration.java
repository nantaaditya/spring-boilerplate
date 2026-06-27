package com.nantaaditya.example.properties.embedded;

import com.nantaaditya.example.model.constant.AsyncRejectedStrategy;
import org.springframework.boot.context.properties.bind.DefaultValue;

public record AsyncConfiguration(
    int corePoolSize,
    int maxPoolSize,
    int queueCapacity,
    int keepAliveSeconds,
    String threadNamePrefix,
    @DefaultValue("LOG_AND_DROP") AsyncRejectedStrategy rejectedTaskStrategy
) {

}
