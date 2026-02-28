package com.nantaaditya.example.properties.embedded;

public record ClientNetworkConfiguration(
    int socketTimeOut,
    int timeToLive,
    int validateAfterInactivity,
    int evictIdleConnection
) {

}
